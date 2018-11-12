//
//  AESCrypt.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 21.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

class AESCrypt {
    
    var SHARED_IV: String = "oklemenz07101214"
    
    var key: [UInt8]!
    var iv: [UInt8]!
    
    init(passcode : String) {
        self.key = Crypto.saltKey(key: passcode)
        self.iv = [UInt8](SHARED_IV.utf8)
    }
    
    func encrypt(_ plainText: String) -> String {
        let bytes = plainText.utf8.map({$0})
        if let encryptedBytes = Crypto.encrypt(bytes, key: key, iv: iv) {
            return Crypto.toBase64(encryptedBytes)
        }
        return ""
    }
    
    func decrypt(_ cryptedText: String) -> String {
        if let bytes = Crypto.fromBase64(cryptedText) {
            if let decryptedBytes = Crypto.decrypt(bytes, key: key, iv: iv) {
                return String(bytes: decryptedBytes, encoding: .utf8)!
            }
        }
        return ""
    }
}
