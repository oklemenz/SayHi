//
//  SecureStore.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 20.01.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import KeychainAccess
import SwiftyJSON
import FirebaseAnalytics

let AppInitialized = "\(Namespace).AppInit"
let CryptInfoField = "\(Namespace).CryptInfo"
let UserDataField  = "\(Namespace).UserData"
let TouchIDField   = "\(Namespace).UseTouchID"
let SpaceField     = "\(Namespace).Space"

#if DEBUG
let StandardSpace = "Dev"
#else
let StandardSpace = "Standard"
#endif

class SecureStore {
    
    static let instance = SecureStore()
    
    let keychain = Keychain(service: Namespace)
    
    var key: [UInt8] = []
    var iv: [UInt8] = []
    var isOpen: Bool = false
    
    init() {
    }

    func open() throws {
        if !isOpen {
            var cryptInfo: String?
            if SecureStore.appInitialized() {
                if Platform.isDevice {
                    let keychain = self.keychain.authenticationPrompt("Access your data".localized)
                    cryptInfo = try keychain.get(CryptInfoField)
                } else {
                    cryptInfo = UserDefaults.standard.string(forKey: CryptInfoField)
                }
            }
            if let cryptInfo = cryptInfo {
                if let jsonData = cryptInfo.data(using: String.Encoding.utf8, allowLossyConversion: false) {
                    let cryptInfoJSON = JSON(data: jsonData)
                    let raw = cryptInfoJSON.rawValue as! [String: Any]
                    key = raw["key"] as! [UInt8]
                    iv = raw["iv"] as! [UInt8]
                }
            } else {
                key = Crypto.generateRandom(length: Crypto.keySize)
                iv = Crypto.generateRandom(length: Crypto.blockSize)
                try storeCryptInfo(key: key, iv: iv)
            }
            isOpen = !key.isEmpty && !iv.isEmpty
        }
    }
    
    private func storeCryptInfo(key: [UInt8], iv: [UInt8]) throws {
        let cryptInfoRaw: [String:[UInt8]] = [
            "key": key,
            "iv": iv
        ]
        let cryptInfoJSON = JSON(cryptInfoRaw)
        let cryptInfo = cryptInfoJSON.rawString(String.Encoding.utf8, options: JSONSerialization.WritingOptions(rawValue: 0))!
        if Platform.isDevice {
            let keychain : Keychain?
            if SecureStore.touchIDUsed {
                keychain = self.keychain.accessibility(.whenUnlocked, authenticationPolicy: .userPresence)
            } else {
                keychain = self.keychain.accessibility(.whenUnlocked)
            }
            // Remove key first, otherwise authentication will be triggered on existing item (=> prompt)
            try keychain?.remove(CryptInfoField)
            try keychain?.set(cryptInfo, key: CryptInfoField)
        } else {
            UserDefaults.standard.set(cryptInfo, forKey: CryptInfoField)
            UserDefaults.standard.synchronize()
        }
    }
    
    func close() {
        iv = []
        key = []
        isOpen = false
    }
    
    func store(_ data: String) throws {
        try open()
        if isOpen {
            if let encryptedBytes = Crypto.encrypt(data.utf8.map({$0}), key: key, iv: iv) {
                UserDefaults.standard.set(encryptedBytes, forKey: UserDataField)
                UserDefaults.standard.synchronize()
            }
        }
    }
    
    func load() throws -> String? {
        try open()
        if isOpen {
            if let bytes = UserDefaults.standard.array(forKey: UserDataField) as? [UInt8] {
                if let decryptedBytes = Crypto.decrypt(bytes, key: key, iv: iv) {
                    return String(bytes: decryptedBytes, encoding: .utf8)!
                }
            }
        }
        return nil
    }
    
    func setUseTouchID(_ state: Bool) {
        do {
            UserDefaults.standard.set(state, forKey: TouchIDField)
            UserDefaults.standard.synchronize()
            if isOpen {
                try storeCryptInfo(key: key, iv: iv)
            }
        } catch let error {
            print(error)
        }
    }
    
    static var touchIDUsed : Bool {
        return Platform.isDevice && authEnabled() && UserDefaults.standard.bool(forKey: TouchIDField)
    }

    static func setAppInitialized() {
        UserDefaults.standard.set(true, forKey: AppInitialized)
        UserDefaults.standard.synchronize()
    }
    
    static func appInitialized() -> Bool {
        return UserDefaults.standard.bool(forKey: AppInitialized)
    }
    
    static var space: String {
        if let space = UserDefaults.standard.string(forKey: SpaceField) {
            return space
        }
        switchSpace(StandardSpace, suppressNotification: true)
        return StandardSpace
    }
    
    static var spaceRefName: String {
        return space.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
    
    static func switchSpace(_ newSpace: String, suppressNotification: Bool = false) {
        let currentSpace = UserDefaults.standard.string(forKey: SpaceField)
        if currentSpace != newSpace {
            UserDefaults.standard.set(newSpace, forKey: SpaceField)
            UserDefaults.standard.synchronize()
            UserData.instance.spaceSwitched(newSpace)
            FirebaseAnalytics.Analytics.setUserProperty(spaceRefName, forName: "space")
            if !suppressNotification {
                Analytics.instance.logSpaceSwitched(newSpace)
                NotificationCenter.default.post(name: SpaceSwitchedNotification, object: nil)
            }
        }
    }
    
    static func switchToStandardSpace() {
        switchSpace(StandardSpace)
    }
}
