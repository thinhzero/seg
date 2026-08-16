use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct EmvData {
    pub card_brand: String,
    pub aid: String,
    pub application_label: String,
    pub pan_masked: String,
    pub expiry_date: String,
    pub cardholder_name: String,
    pub country_code: String,
    pub currency_code: String,
    pub language: String,
    pub tags: Vec<EmvTag>,
    pub raw_responses: Vec<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EmvTag {
    pub tag: String,
    pub tag_name: String,
    pub length: usize,
    pub value_hex: String,
    pub value_text: String,
}

pub fn parse_emv(raw_apdu_responses_json: &str) -> Result<EmvData, String> {
    let responses: Vec<String> = serde_json::from_str(raw_apdu_responses_json)
        .map_err(|e| format!("Failed to parse input: {}", e))?;
    
    let mut all_tags = Vec::new();
    for resp in &responses {
        if let Ok(bytes) = hex_decode(resp) {
            let mut offset = 0;
            while offset < bytes.len() {
                if let Some((tag, tag_name, len, value, next_offset)) = parse_tlv(&bytes, offset) {
                    all_tags.push(EmvTag {
                        tag,
                        tag_name,
                        length: len,
                        value_hex: hex_encode(value),
                        value_text: String::from_utf8_lossy(value).into_owned(),
                    });
                    offset = next_offset;
                } else {
                    break;
                }
            }
        }
    }

    let mut card_brand = String::new();
    let mut aid = String::new();
    let mut app_label = String::new();
    let mut pan_masked = String::new();
    let mut expiry_date = String::new();
    let mut cardholder_name = String::new();
    let mut country_code = String::new();
    let mut currency_code = String::new();
    let mut language = String::new();

    for tag in &all_tags {
        match tag.tag.as_str() {
            "4F" | "9F06" => {
                aid = tag.value_hex.clone();
                card_brand = identify_brand(&aid);
            },
            "50" => app_label = tag.value_text.clone(),
            "5A" => pan_masked = mask_pan(&tag.value_hex),
            "5F20" => cardholder_name = tag.value_text.clone(),
            "5F24" => {
                let val = &tag.value_hex;
                if val.len() >= 4 {
                    expiry_date = format!("{}/{}", &val[2..4], &val[0..2]);
                }
            },
            "5F28" => country_code = tag.value_hex.clone(),
            "5F2D" => language = tag.value_text.clone(),
            "9F42" => currency_code = tag.value_hex.clone(),
            _ => {}
        }
    }

    Ok(EmvData {
        card_brand,
        aid,
        application_label: app_label,
        pan_masked,
        expiry_date,
        cardholder_name,
        country_code,
        currency_code,
        language,
        tags: all_tags,
        raw_responses: responses,
    })
}

fn hex_decode(hex: &str) -> Result<Vec<u8>, String> {
    let clean_hex: String = hex.chars().filter(|c| c.is_ascii_hexdigit()).collect();
    if clean_hex.len() % 2 != 0 {
        return Err("Invalid hex length".into());
    }
    let mut bytes = Vec::new();
    for i in (0..clean_hex.len()).step_by(2) {
        let byte = u8::from_str_radix(&clean_hex[i..i+2], 16).map_err(|e| e.to_string())?;
        bytes.push(byte);
    }
    Ok(bytes)
}

fn hex_encode(data: &[u8]) -> String {
    data.iter().map(|b| format!("{:02X}", b)).collect()
}

fn parse_tlv(bytes: &[u8], mut offset: usize) -> Option<(String, String, usize, &[u8], usize)> {
    if offset >= bytes.len() { return None; }
    
    let mut tag_bytes = vec![bytes[offset]];
    let tag_byte = bytes[offset];
    offset += 1;
    
    if (tag_byte & 0x1F) == 0x1F {
        if offset >= bytes.len() { return None; }
        tag_bytes.push(bytes[offset]);
        offset += 1;
        while (tag_bytes.last().unwrap() & 0x80) == 0x80 {
            if offset >= bytes.len() { return None; }
            tag_bytes.push(bytes[offset]);
            offset += 1;
        }
    }
    let tag = hex_encode(&tag_bytes);
    
    if offset >= bytes.len() { return None; }
    let mut len = bytes[offset] as usize;
    offset += 1;
    
    if (len & 0x80) == 0x80 {
        let num_len_bytes = len & 0x7F;
        if num_len_bytes > 0 {
            if offset + num_len_bytes > bytes.len() { return None; }
            len = 0;
            for _ in 0..num_len_bytes {
                len = (len << 8) | (bytes[offset] as usize);
                offset += 1;
            }
        } else {
            len = 0;
        }
    }
    
    let is_constructed = (tag_bytes[0] & 0x20) != 0;
    
    let mut value_end = offset + len;
    if value_end > bytes.len() { 
        value_end = bytes.len();
        len = value_end - offset;
    }
    let value = &bytes[offset..value_end];
    
    let tag_name = get_tag_name(&tag);
    
    if is_constructed {
        Some((tag, tag_name, len, &[], offset))
    } else {
        Some((tag, tag_name, len, value, value_end))
    }
}

fn get_tag_name(tag: &str) -> String {
    match tag {
        "4F" | "9F06" => "Application Identifier (AID)",
        "50" => "Application Label",
        "57" => "Track 2 Equivalent Data",
        "5A" => "Application Primary Account Number (PAN)",
        "5F20" => "Cardholder Name",
        "5F24" => "Application Expiration Date",
        "5F25" => "Application Effective Date",
        "5F28" => "Issuer Country Code",
        "5F2D" => "Language Preference",
        "5F34" => "Application PAN Sequence Number",
        "6F" => "File Control Information (FCI) Template",
        "70" => "EMV Proprietary Template",
        "84" => "Dedicated File (DF) Name",
        "87" => "Application Priority Indicator",
        "88" => "Short File Identifier (SFI)",
        "8C" => "Card Risk Management Data Object List 1 (CDOL1)",
        "8D" => "Card Risk Management Data Object List 2 (CDOL2)",
        "9F02" => "Amount, Authorised (Numeric)",
        "9F07" => "Application Usage Control",
        "9F08" => "Application Version Number",
        "9F0D" => "Issuer Action Code - Default",
        "9F0E" => "Issuer Action Code - Denial",
        "9F0F" => "Issuer Action Code - Online",
        "9F11" => "Issuer Code Table Index",
        "9F12" => "Application Preferred Name",
        "9F1F" => "Track 1 Discretionary Data",
        "9F26" => "Application Cryptogram",
        "9F27" => "Cryptogram Information Data",
        "9F36" => "Application Transaction Counter (ATC)",
        "9F42" => "Application Currency Code",
        "9F44" => "Application Currency Exponent",
        "9F4A" => "Static Data Authentication Tag List",
        _ => "Unknown Tag",
    }.to_string()
}

fn identify_brand(aid: &str) -> String {
    if aid.starts_with("A0000000031010") || aid.starts_with("A0000000032010") {
        "Visa".to_string()
    } else if aid.starts_with("A0000000041010") {
        "Mastercard".to_string()
    } else if aid.starts_with("A0000000651010") {
        "JCB".to_string()
    } else if aid.starts_with("A000000025010104") {
        "AMEX".to_string()
    } else if aid.starts_with("A0000001523010") {
        "Discover".to_string()
    } else if aid.starts_with("A0000006723010") {
        "TROY".to_string()
    } else {
        "Unknown Brand".to_string()
    }
}

fn mask_pan(pan: &str) -> String {
    let clean = pan.replace("F", "");
    if clean.len() >= 10 {
        let first_6 = &clean[0..6];
        let last_4 = &clean[clean.len()-4..];
        let masked = "*".repeat(clean.len() - 10);
        format!("{}{}{}", first_6, masked, last_4)
    } else {
        clean
    }
}
