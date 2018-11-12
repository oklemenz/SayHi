//
//  Settings.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 20.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation
import FirebaseAnalytics

let SettingsField = "\(Namespace).Settings"

class Settings {
    
    static let instance = Settings()
    
    var defaultData: [String: Any] = [
        "accentColor": AccentColorDefault,
        "disableHelp": false,
        "disableHelpQR": false,
        "disableHighscoreShare": false,
        "disableHighscoreShow": false,
        "disableNewCategories": false,
        "disableNewProfiles": false,
        "disableNewTags": false,
        "disableRecordAnalytics": false,
        "disableRecordAnalyticsDB": false,
        "disableSettingsHandshake": false,
        "disableSettingsLanguage": false,
        "disableSettingsMatchMode": false,
        "favoriteLanguages": BundleLangCodes,
        "gradientColor1": GradientColor1Default,
        "gradientColor2": GradientColor2Default,
        "highscoreLocal": "",
        //"leftLabel": nil,
        //"leftLabelFallback": nil,
        //"logo": nil
        "logoPlain": false,
        "logoZoom": 1.0,
        "maintenance": false,
        "primaryReference": false,
        "queryLimit": UInt(100),
        //"rightLabel": nil,
        //"rightLabelFallback": nil,
        "scoreCount": ["leftLeft", "rightRight", "leftRight", "rightLeft"],
        //"settingsHandshake": nil,
        //"settingsLanguage": nil,
        //"settingsMatchMode": nil,
        //"terminology": nil
    ]
    
    var data: [String: Any]
    var loaded: Bool = false
    var spaceSwitch: Bool = false
    
    init() {
        data = defaultData
        load()
    }
    
    func reset() {
        _logo = nil
        data = defaultData
        UserDefaults.standard.removeObject(forKey: SettingsField)
        UserDefaults.standard.synchronize()
        AnalyticsConfiguration.shared().setAnalyticsCollectionEnabled(true)
    }
    
    func load() {
        if loaded {
            return
        }
        if let loadedData = UserDefaults.standard.object(forKey: SettingsField) as? [String: Any] {
            data = loadedData
        }
        loaded = true
    }
    
    func store() {
        UserDefaults.standard.set(data, forKey: SettingsField)
        UserDefaults.standard.synchronize()
    }
    
    func update(_ newData: [String: Any], _ spaceSwitch: Bool = false) {
        reset()
        
        for (name, value) in newData {
            var newValue: Any? = nil
            switch (name) {
                case "accentColor":
                    newValue = value as? String
                case "disableHelp":
                    newValue = value as? Bool
                case "disableHelpQR":
                    newValue = value as? Bool
                case "disableHighscoreShare":
                    newValue = value as? Bool
                case "disableHighscoreShow":
                    newValue = value as? Bool
                case "disableNewCategories":
                    newValue = value as? Bool
                case "disableNewProfiles":
                    newValue = value as? Bool
                case "disableNewTags":
                    newValue = value as? Bool
                case "disableRecordAnalytics":
                    newValue = value as? Bool
                    if let newValue = newValue as? Bool {
                        AnalyticsConfiguration.shared().setAnalyticsCollectionEnabled(!newValue)
                    }
                case "disableRecordAnalyticsDB":
                    newValue = value as? Bool
                case "disableSettingsHandshake":
                    newValue = value as? Bool
                case "disableSettingsLanguage":
                    newValue = value as? Bool
                case "disableSettingsMatchMode":
                    newValue = value as? Bool
                case "favoriteLanguages":
                    newValue = (value as? String) != nil ? (value as! String).components(separatedBy: ",") : nil
                case "gradientColor1":
                    newValue = value as? String
                case "gradientColor2":
                    newValue = value as? String
                case "highscoreLocal":
                    newValue = value as? String
                case "leftLabel":
                    newValue = value as? String
                case "leftLabelFallback":
                    newValue = value as? String
                case "logo":
                    newValue = value as? String
                case "logoPlain":
                    newValue = value as? Bool
                case "logoZoom":
                    newValue = value as? Float
                case "maintenance":
                    newValue = value as? Bool
                case "primaryReference":
                    newValue = value as? Bool
                case "queryLimit":
                    newValue = value as? UInt
                case "rightLabel":
                    newValue = value as? String
                case "rightLabelFallback":
                    newValue = value as? String
                case "scoreCount":
                    newValue = (value as? String) != nil ? (value as! String).components(separatedBy: ",") : nil
                case "settingsHandshake":
                    newValue = value as? Bool
                case "settingsLanguage":
                    newValue = value as? String
                case "settingsMatchMode":
                    newValue = value as? String
                case "terminology":
                    newValue = value as? String
                default:
                    break
            }
            if let newValue = newValue {
                data[name] = newValue
            }
        }
        
        if !spaceSwitch {
            if !disableSettingsHandshake {
                data["settingsHandshake"] = nil
            }
            if !disableSettingsLanguage {
                data["settingsLanguage"] = nil
            }
            if !disableSettingsMatchMode {
                data["settingsMatchMode"] = nil
            }
        }
        
        store()
        self.spaceSwitch = spaceSwitch
    }
    
    var configItems: [String: String] {
        var configItems = [String: String]()
        if let handshake = settingsHandshake {
            configItems["handshake"] = handshake ? "true" : "false"
        }
        if let language = settingsLanguage {
            configItems["language"] = language
        }
        if let matchMode = settingsMatchModeExternalCode {
            configItems["matchMode"] = matchMode
        }
        return configItems
    }

    var accentColor: String {
        return data["accentColor"] as! String
    }
    
    var disableHelp: Bool {
        return data["disableHelp"] as! Bool
    }
    
    var disableHelpQR: Bool {
        return data["disableHelpQR"] as! Bool
    }

    var disableHighscoreShare: Bool {
        return data["disableHighscoreShare"] as! Bool
    }
    
    var disableHighscoreShow: Bool {
        return data["disableHighscoreShow"] as! Bool
    }

    var disableNewCategories: Bool {
        return data["disableNewCategories"] as! Bool
    }
    
    var disableNewProfiles: Bool {
        return data["disableNewProfiles"] as! Bool
    }
    
    var disableNewTags: Bool {
        return data["disableNewTags"] as! Bool
    }
    
    var disableRecordAnalytics: Bool {
        return data["disableRecordAnalytics"] as! Bool
    }
    
    var disableRecordAnalyticsDB: Bool {
        return data["disableRecordAnalyticsDB"] as! Bool
    }

    var disableSettingsHandshake: Bool {
        return data["disableSettingsHandshake"] as! Bool
    }
    
    var disableSettingsLanguage: Bool {
        return data["disableSettingsLanguage"] as! Bool
    }
    
    var disableSettingsMatchMode: Bool {
        return data["disableSettingsMatchMode"] as! Bool
    }

    var favoriteLanguages: [String] {
        return data["favoriteLanguages"] as! [String]
    }
    
    var gradientColor1: String {
        return data["gradientColor1"] as! String
    }
    
    var gradientColor2: String {
        return data["gradientColor2"] as! String
    }
    
    var highscoreLocal: String {
        return data["highscoreLocal"] as! String
    }

    var leftLabel: String? {
        return data["leftLabel"] as? String
    }

    var leftLabelFallback: String? {
        return data["leftLabelFallback"] as? String
    }

    var _logo: UIImage?
    var logo: UIImage? {
        if _logo != nil {
            return _logo
        }
        if let logoData = data["logo"] as? String {
            if let logoData = Data(base64Encoded: logoData) {
                if var logoImage = UIImage(data: logoData) {
                    logoImage = UIImage(cgImage: logoImage.cgImage!, scale: UIScreen.main.screenScale, orientation: .up)
                    _logo = logoImage
                }
            }
        }
        return _logo
    }
    
    var logoPlain: Bool {
        return data["logoPlain"] as! Bool
    }
    
    var logoZoom: Float {
        return data["logoZoom"] as! Float
    }
    
    var maintenance: Bool {
        return data["maintenance"] as! Bool
    }
    
    var primaryReference: Bool {
        return data["primaryReference"] as! Bool
    }
    
    var queryLimit: UInt {
        return data["queryLimit"] as! UInt
    }
    
    var rightLabel: String? {
        return data["rightLabel"] as? String
    }
    
    var rightLabelFallback: String? {
        return data["rightLabelFallback"] as? String
    }
    
    var scoreCount: [String] {
        return data["scoreCount"] as! [String]
    }
    
    var settingsHandshake: Bool? {
        return data["settingsHandshake"] as? Bool
    }
    
    var settingsLanguage: String? {
        return data["settingsLanguage"] as? String
    }
    
    var settingsMatchModeExternalCode: String? {
        return data["settingsMatchMode"] as? String
    }
    var settingsMatchMode: MatchMode? {
        if let matchModeExternalCode = settingsMatchModeExternalCode {
            return MatchMode.fromDescription(matchModeExternalCode)
        }
        return nil
    }
    
    var terminology: String? {
        return data["terminology"] as? String
    }
}
