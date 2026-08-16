pub fn create_text_record(text: &str, locale: &str) -> Vec<u8> {
    let mut payload = Vec::new();
    let lang_bytes = locale.as_bytes();
    let status_byte = (lang_bytes.len() as u8) & 0x3F; // UTF-8, length
    payload.push(status_byte);
    payload.extend_from_slice(lang_bytes);
    payload.extend_from_slice(text.as_bytes());

    let mut record = Vec::new();
    // MB=1, ME=1, SR=1, TNF=1 (Well-Known)
    record.push(0xD1);
    record.push(0x01); // Type Length
    record.push(payload.len() as u8); // Payload Length
    record.push(b'T'); // Type
    record.extend(payload);

    record
}

pub fn create_uri_record(uri: &str) -> Vec<u8> {
    let mut payload = Vec::new();
    
    // Find prefix
    let prefixes = [
        "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
        "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
        "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
        "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:",
        "urn:epc:", "urn:nfc:"
    ];
    
    let mut prefix_code = 0x00;
    let mut uri_remainder = uri;
    
    for (i, p) in prefixes.iter().enumerate() {
        if uri.starts_with(p) {
            prefix_code = (i + 1) as u8;
            uri_remainder = &uri[p.len()..];
            break;
        }
    }
    
    payload.push(prefix_code);
    payload.extend_from_slice(uri_remainder.as_bytes());

    let mut record = Vec::new();
    // MB=1, ME=1, SR=1, TNF=1 (Well-Known)
    record.push(0xD1);
    record.push(0x01); // Type length
    record.push(payload.len() as u8); // Payload Length
    record.push(b'U'); // Type
    record.extend(payload);

    record
}
