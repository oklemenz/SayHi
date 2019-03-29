//
//  Match.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 15.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import SwiftyJSON

class Match : NSObject {
    
    let year : Int = Calendar.current.dateComponents([.year], from: Date()).year!
    
    var date: Date = Date()
    
    var mode: MatchMode = .open
    var handshake: Bool = false
    var profileName: String = ""
    var matchLangCode: String = ""
    var relationType: RelationType = .none
    var profilePosTagCount: Int = 0
    var profileNegTagCount: Int = 0
    
    var locationLatitude: String = ""
    var locationLongitude: String = ""
    var locationName: String = ""
    var locationStreet: String = ""
    var locationCity: String = ""
    var locationCountry: String = ""
    
    var langCode: String = ""
    var firstName: String = ""
    var gender: Gender = .none
    var birthYear: Int = 0
    var status: String = ""
    var installationUUID: String = ""
    var messagePosTagCount: Int = 0
    var messageNegTagCount: Int = 0
    
    var bothPosTags: [Tag] = []
    var bothNegTags: [Tag] = []
    var onlyPosTags: [Tag] = []
    var onlyNegTags: [Tag] = []
    
    var bothPosScore: Int = 0
    var bothNegScore: Int = 0
    var onlyPosScore: Int = 0
    var onlyNegScore: Int = 0
    
    var counted: Bool = true
    
    var score: Int {
        return bothPosScore + bothNegScore + onlyPosScore + onlyNegScore
    }
    
    var age : Int {
        if birthYear >= BaseYear && birthYear <= year {
            return year - birthYear
        }
        return 0
    }
    
    var bothPosTagKeys : [String] {
        return bothPosTags.map({ (tag) in
            return tag.key
        })
    }
    
    var bothNegTagKeys : [String] {
        return bothNegTags.map({ (tag) in
            return tag.key
        })
    }
    
    var onlyPosTagKeys : [String] {
        return onlyPosTags.map({ (tag) in
            return tag.key
        })
    }
    
    var onlyNegTagKeys : [String] {
        return onlyNegTags.map({ (tag) in
            return tag.key
        })
    }
    
    var bothPosTagEffectiveKeys : [String] {
        return bothPosTags.map({ (tag) in
            return tag.effectiveKey
        })
    }
    
    var bothNegTagEffectiveKeys : [String] {
        return bothNegTags.map({ (tag) in
            return tag.effectiveKey
        })
    }
    
    var onlyPosTagEffectiveKeys : [String] {
        return onlyPosTags.map({ (tag) in
            return tag.effectiveKey
        })
    }
    
    var onlyNegTagEffectiveKeys : [String] {
        return onlyNegTags.map({ (tag) in
            return tag.effectiveKey
        })
    }
    
    var bothPosTagNames : [String] {
        return bothPosTags.map({ (tag) in
            return tag.name
        })
    }
    
    var bothNegTagNames : [String] {
        return bothNegTags.map({ (tag) in
            return tag.name
        })
    }
    
    var onlyPosTagNames : [String] {
        return onlyPosTags.map({ (tag) in
            return tag.name
        })
    }
    
    var onlyNegTagNames : [String] {
        return onlyNegTags.map({ (tag) in
            return tag.name
        })
    }
    
    func toJSON() -> JSON {
        var raw: [String:Any] = [:]
        raw["d"] = date.iso
        raw["m"] = mode.rawValue
        raw["mh"] = handshake
        raw["p"] = profileName
        raw["ml"] = matchLangCode
        raw["r"] = relationType.rawValue
        raw["ppt"] = profilePosTagCount
        raw["pnt"] = profileNegTagCount
        raw["la"] = locationLatitude
        raw["lo"] = locationLongitude
        raw["ln"] = locationName
        raw["ls"] = locationStreet
        raw["lci"] = locationCity
        raw["lco"] = locationCountry
        raw["l"] = langCode
        raw["n"] = firstName
        raw["g"] = gender.rawValue
        raw["y"] = birthYear
        raw["s"] = status
        raw["i"] = installationUUID
        raw["mpt"] = messagePosTagCount
        raw["mnt"] = messageNegTagCount
        raw["bpt"] = bothPosTagKeys
        raw["bnt"] = bothNegTagKeys
        raw["opt"] = onlyPosTagKeys
        raw["ont"] = onlyNegTagKeys
        raw["sbp"] = bothPosScore
        raw["sbn"] = bothNegScore
        raw["sop"] = onlyPosScore
        raw["son"] = onlyNegScore
        raw["c"] = counted
        return JSON(raw)
    }
    
    func toJSONString() -> String {
        return toJSON().rawString(String.Encoding.utf8, options: JSONSerialization.WritingOptions(rawValue: 0))!
    }
    
    static func fromJSON(json : JSON) -> Match {
        var raw = json.rawValue as! [String:Any]
        let match = Match()
        
        match.date = (raw["d"] as! String).dateFromISO!
        
        match.mode = MatchMode(rawValue: raw["m"] as? Int ?? MatchMode.open.rawValue) ?? .open
        match.handshake = raw["mh"] as? Bool ?? false
        match.profileName = raw["p"] as! String
        match.matchLangCode = raw["ml"] as? String ?? ""
        match.relationType = RelationType(rawValue: raw["r"] as? String ?? RelationType.none.rawValue) ?? .none
        match.profilePosTagCount = raw["ppt"] as? Int ?? 0
        match.profileNegTagCount = raw["pnt"] as? Int ?? 0
        match.locationLatitude = raw["la"] as? String ?? ""
        match.locationLongitude = raw["lo"] as? String ?? ""
        match.locationName = raw["ln"] as? String ?? ""
        match.locationStreet = raw["ls"] as? String ?? ""
        match.locationCity = raw["lci"] as? String ?? ""
        match.locationCountry = raw["lco"] as? String ?? ""
        
        match.langCode = raw["l"] as? String ?? ""
        match.firstName = raw["n"] as? String ?? ""
        match.gender = Gender(rawValue: raw["g"] as? String ?? Gender.none.rawValue)!
        match.birthYear = raw["y"] as? Int ?? BaseYear
        match.status = raw["s"] as? String ?? ""
        match.installationUUID = raw["i"] as? String ?? ""
        match.messagePosTagCount = raw["mpt"] as? Int ?? 0
        match.messageNegTagCount = raw["mnt"] as? Int ?? 0
        
        for bothPosTagKey in json["bpt"] {
            if let tag = Tag.lookupKey(bothPosTagKey.1.rawValue as! String) {
                match.bothPosTags.append(tag)
            }
        }
        
        for bothNegTagKey in json["bnt"] {
            if let tag = Tag.lookupKey(bothNegTagKey.1.rawValue as! String) {
                match.bothNegTags.append(tag)
            }
        }
        
        for onlyPosTagKey in json["opt"] {
            if let tag = Tag.lookupKey(onlyPosTagKey.1.rawValue as! String) {
                match.onlyPosTags.append(tag)
            }
        }
        
        for onlyNegTagKey in json["ont"] {
            if let tag = Tag.lookupKey(onlyNegTagKey.1.rawValue as! String) {
                match.onlyNegTags.append(tag)
            }
        }
        
        match.bothPosScore = raw["sbp"] as? Int ?? 0
        match.bothNegScore = raw["sbn"] as? Int ?? 0
        match.onlyPosScore = raw["sop"] as? Int ?? 0
        match.onlyNegScore = raw["son"] as? Int ?? 0
        
        match.counted = raw["c"] as? Bool ?? true
        
        return match
    }
    
    static func fromJSONString(jsonString : String) -> Match? {
        if let data = jsonString.data(using: String.Encoding.utf8, allowLossyConversion: false) {
            return Match.fromJSON(json: JSON(data: data))
        }
        return nil
    }
    
    static func calculateMatch(profile: Profile, message: Message) -> Match {
        let match = Match()
        match.date = Date()
        
        match.mode = MatchMode(rawValue: min((profile.matchMode ?? UserData.instance.matchMode).rawValue, message.matchMode.rawValue)) ?? .open
        if Settings.instance.disableSettingsMatchMode && Settings.instance.settingsMatchMode != nil {
            match.mode = Settings.instance.settingsMatchMode!
        }
        match.handshake = UserData.instance.matchHandshake || message.matchHandshake
        match.profileName = profile.name
        match.matchLangCode = UserData.instance.langCode
        match.relationType = profile.relationType
        match.profilePosTagCount = profile.posTags.count
        match.profileNegTagCount = profile.negTags.count
        
        match.langCode = message.langCode
        match.firstName = message.firstName
        match.gender = message.gender
        match.birthYear = message.birthYear
        match.status = message.status
        match.installationUUID = message.installationUUID
        match.messagePosTagCount = message.posTags.count
        match.messageNegTagCount = message.negTags.count
        
        match.bothPosTags = profile.posTags.filter({ (tag) -> Bool in
            return message.posTags.contains(tag.effectiveKey)
        })
        match.bothPosTags.sort { (tag1, tag2) -> Bool in
            return (message.posTags.firstIndex(of: tag1.effectiveKey) ?? 0) - (message.posTags.firstIndex(of: tag2.effectiveKey) ?? 0) < 0
        }
        
        match.bothNegTags = profile.negTags.filter({ (tag) -> Bool in
            return message.negTags.contains(tag.effectiveKey)
        })
        match.bothNegTags.sort { (tag1, tag2) -> Bool in
            return (message.negTags.firstIndex(of: tag1.effectiveKey) ?? 0) - (message.negTags.firstIndex(of: tag2.effectiveKey) ?? 0) < 0
        }
        
        if match.mode == .adapt || match.mode == .open {
            match.onlyPosTags = profile.posTags.filter({ (tag) -> Bool in
                return message.negTags.contains(tag.effectiveKey)
            })
            match.onlyPosTags.sort { (tag1, tag2) -> Bool in
                return (message.negTags.firstIndex(of: tag1.effectiveKey) ?? 0) - (message.negTags.firstIndex(of: tag2.effectiveKey) ?? 0) < 0
            }
        }
        
        if match.mode == .tries || match.mode == .open {
            match.onlyNegTags = profile.negTags.filter({ (tag) -> Bool in
                return message.posTags.contains(tag.effectiveKey)
            })
            match.onlyNegTags.sort { (tag1, tag2) -> Bool in
                return (message.posTags.firstIndex(of: tag1.effectiveKey) ?? 0) - (message.posTags.firstIndex(of: tag2.effectiveKey) ?? 0) < 0
            }
        }
        
        if Settings.instance.scoreCount.contains("leftLeft") {
            match.bothPosScore = match.bothPosTags.count
        }
        if Settings.instance.scoreCount.contains("rightRight") {
            match.bothNegScore = match.bothNegTags.count
        }
        if Settings.instance.scoreCount.contains("leftRight") {
            match.onlyPosScore = match.onlyPosTags.count
        }
        if Settings.instance.scoreCount.contains("rightLeft") {
            match.onlyNegScore = match.onlyNegTags.count
        }
        
        return match
    }
}
