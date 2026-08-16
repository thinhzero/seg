use jni::JNIEnv;
use jni::objects::{JClass, JString, JByteArray};
use jni::sys::{jstring, jbyteArray, jboolean, jbyte};
use log::info;

mod ndef;
mod emv;
mod writer;
mod tag_info;

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_nativeInit<'local>(
    mut _env: JNIEnv<'local>,
    _class: JClass<'local>,
) {
    #[cfg(target_os = "android")]
    {
        android_logger::init_once(
            android_logger::Config::default().with_max_level(log::LevelFilter::Trace),
        );
    }
    info!("Rust logger initialized!");
}

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_parseNdefMessage<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    raw_bytes: JByteArray<'local>,
) -> jstring {
    let bytes = env.convert_byte_array(&raw_bytes).unwrap_or_default();
    let result = ndef::parse_ndef(&bytes);
    
    let json = match result {
        Ok(msg) => serde_json::to_string(&msg).unwrap_or_else(|_| "{}".to_string()),
        Err(e) => format!("{{\"error\":\"{}\"}}", e),
    };
    
    env.new_string(json).expect("Couldn't create java string!").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_parseEmvData<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    raw_apdu_responses: JString<'local>,
) -> jstring {
    let apdu_str: String = env.get_string(&raw_apdu_responses).unwrap().into();
    let result = emv::parse_emv(&apdu_str);
    
    let json = match result {
        Ok(data) => serde_json::to_string(&data).unwrap_or_else(|_| "{}".to_string()),
        Err(e) => format!("{{\"error\":\"{}\"}}", e),
    };
    
    env.new_string(json).expect("Couldn't create java string!").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_analyzeTag<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    uid: JByteArray<'local>,
    tech_list: JString<'local>,
    atqa: JByteArray<'local>,
    sak: jbyte,
) -> jstring {
    let uid_bytes = env.convert_byte_array(&uid).unwrap_or_default();
    let atqa_bytes = env.convert_byte_array(&atqa).unwrap_or_default();
    let tech_list_str: String = env.get_string(&tech_list).unwrap().into();
    let sak_u8 = sak as u8;
    
    let result = tag_info::analyze_tag(&uid_bytes, &tech_list_str, &atqa_bytes, sak_u8);
    
    let json = match result {
        Ok(info) => serde_json::to_string(&info).unwrap_or_else(|_| "{}".to_string()),
        Err(e) => format!("{{\"error\":\"{}\"}}", e),
    };
    
    env.new_string(json).expect("Couldn't create java string!").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_createNdefTextRecord<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    text: JString<'local>,
    locale: JString<'local>,
) -> jbyteArray {
    let text_str: String = env.get_string(&text).unwrap().into();
    let locale_str: String = env.get_string(&locale).unwrap().into();
    
    let record_bytes = writer::create_text_record(&text_str, &locale_str);
    
    env.byte_array_from_slice(&record_bytes).expect("Couldn't create byte array").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_createNdefUriRecord<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    uri: JString<'local>,
) -> jbyteArray {
    let uri_str: String = env.get_string(&uri).unwrap().into();
    
    let record_bytes = writer::create_uri_record(&uri_str);
    
    env.byte_array_from_slice(&record_bytes).expect("Couldn't create byte array").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_thinhzero_seg_RustBridge_checkNfcCapability<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    has_nfc: jboolean,
    is_enabled: jboolean,
) -> jstring {
    let has = has_nfc != 0;
    let enabled = is_enabled != 0;
    
    let json = format!("{{\"has_nfc\":{}, \"is_enabled\":{}}}", has, enabled);
    env.new_string(json).expect("Couldn't create java string!").into_raw()
}
