use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct TagInfo {
    pub uid: String,
    pub tag_type: String,
    pub technology: Vec<String>,
    pub is_writable: bool,
    pub is_formattable: bool,
    pub max_size: usize,
    pub description: String,
}

pub fn analyze_tag(uid: &[u8], tech_list_str: &str, atqa: &[u8], sak: u8) -> Result<TagInfo, String> {
    let uid_hex = uid.iter().map(|b| format!("{:02X}", b)).collect::<Vec<_>>().join("");
    
    let mut tag_type = "Unknown".to_string();
    let mut is_writable = false;
    let mut is_formattable = false;
    let mut max_size = 0;
    
    // Analyze ATQA/SAK
    let atqa_val = if atqa.len() >= 2 {
        ((atqa[1] as u16) << 8) | (atqa[0] as u16)
    } else {
        0
    };
    
    if sak == 0x08 && atqa_val == 0x0004 {
        tag_type = "MIFARE Classic 1K".to_string();
        max_size = 1024;
        is_writable = true;
    } else if sak == 0x18 && atqa_val == 0x0002 {
        tag_type = "MIFARE Classic 4K".to_string();
        max_size = 4096;
        is_writable = true;
    } else if sak == 0x00 && atqa_val == 0x0044 {
        tag_type = "MIFARE Ultralight".to_string();
        max_size = 64;
        is_writable = true;
        is_formattable = true;
    } else if sak == 0x20 {
        tag_type = "MIFARE DESFire".to_string();
        max_size = 4096; // Variable
    } else if sak == 0x28 {
        tag_type = "ISO 14443-4".to_string();
    }
    
    if tech_list_str.contains("NdefFormatable") {
        is_formattable = true;
    }
    if tech_list_str.contains("Ndef") && !tech_list_str.contains("NdefFormatable") {
        is_writable = true;
    }
    if tech_list_str.contains("NfcF") {
        tag_type = "FeliCa".to_string();
    }
    
    let tech_list: Vec<String> = tech_list_str.split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect();

    Ok(TagInfo {
        uid: uid_hex,
        tag_type,
        technology: tech_list,
        is_writable,
        is_formattable,
        max_size,
        description: "Analyzed tag information".to_string(),
    })
}
