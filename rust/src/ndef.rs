use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct NdefMessage {
    pub records: Vec<NdefRecord>,
    pub total_size: usize,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct NdefRecord {
    pub tnf: u8,
    pub tnf_name: String,
    pub type_name: String,
    pub id: String,
    pub payload_hex: String,
    pub payload_text: String,
    pub payload_size: usize,
}

const URI_PREFIX_MAP: &[&str] = &[
    "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
    "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
    "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
    "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
    "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:",
    "urn:epc:", "urn:nfc:"
];

pub fn parse_ndef(bytes: &[u8]) -> Result<NdefMessage, String> {
    let mut records = Vec::new();
    let mut offset = 0;

    while offset < bytes.len() {
        if offset >= bytes.len() {
            break;
        }
        let header = bytes[offset];
        offset += 1;

        let me = (header & 0x40) != 0;
        let sr = (header & 0x10) != 0;
        let il = (header & 0x08) != 0;
        let tnf = header & 0x07;

        if offset >= bytes.len() { break; }
        let type_length = bytes[offset] as usize;
        offset += 1;

        let payload_length: usize;
        if sr {
            if offset >= bytes.len() { break; }
            payload_length = bytes[offset] as usize;
            offset += 1;
        } else {
            if offset + 4 > bytes.len() { break; }
            payload_length = u32::from_be_bytes([bytes[offset], bytes[offset+1], bytes[offset+2], bytes[offset+3]]) as usize;
            offset += 4;
        }

        let id_length: usize;
        if il {
            if offset >= bytes.len() { break; }
            id_length = bytes[offset] as usize;
            offset += 1;
        } else {
            id_length = 0;
        }

        if offset + type_length > bytes.len() { break; }
        let type_bytes = &bytes[offset..offset+type_length];
        offset += type_length;

        if offset + id_length > bytes.len() { break; }
        let id_bytes = &bytes[offset..offset+id_length];
        offset += id_length;

        if offset + payload_length > bytes.len() { break; }
        let payload_bytes = &bytes[offset..offset+payload_length];
        offset += payload_length;

        let type_name = String::from_utf8_lossy(type_bytes).to_string();
        let id = hex::encode(id_bytes);
        let payload_hex = hex::encode(payload_bytes);
        let mut payload_text = String::new();

        let tnf_name = match tnf {
            0x00 => "Empty",
            0x01 => "Well-Known",
            0x02 => "MIME",
            0x03 => "URI",
            0x04 => "External",
            0x05 => "Unknown",
            0x06 => "Unchanged",
            _ => "Reserved",
        }.to_string();

        if tnf == 0x01 { // Well-Known
            if type_name == "T" {
                if !payload_bytes.is_empty() {
                    let status = payload_bytes[0];
                    let lang_len = (status & 0x3F) as usize;
                    if 1 + lang_len <= payload_bytes.len() {
                        let text_bytes = &payload_bytes[1 + lang_len..];
                        payload_text = String::from_utf8_lossy(text_bytes).to_string();
                    }
                }
            } else if type_name == "U" {
                if !payload_bytes.is_empty() {
                    let prefix_code = payload_bytes[0] as usize;
                    let prefix = if prefix_code < URI_PREFIX_MAP.len() {
                        URI_PREFIX_MAP[prefix_code]
                    } else {
                        ""
                    };
                    let uri_bytes = &payload_bytes[1..];
                    payload_text = format!("{}{}", prefix, String::from_utf8_lossy(uri_bytes));
                }
            } else if type_name == "Sp" {
                payload_text = "Smart Poster".to_string();
            }
        } else if tnf == 0x02 || tnf == 0x03 || tnf == 0x04 {
            payload_text = String::from_utf8_lossy(payload_bytes).to_string();
        }

        records.push(NdefRecord {
            tnf,
            tnf_name,
            type_name,
            id,
            payload_hex,
            payload_text,
            payload_size: payload_length,
        });

        if me {
            break;
        }
    }

    Ok(NdefMessage {
        records,
        total_size: bytes.len(),
    })
}

mod hex {
    pub fn encode(data: &[u8]) -> String {
        data.iter().map(|b| format!("{:02X}", b)).collect::<Vec<_>>().join("")
    }
}
