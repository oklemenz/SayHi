//
//  Profile.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 18.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import SwiftyJSON

class Profile {
    
    var id: Int = 0
    var name: String = ""
    var relationType: RelationType = .none
    var matchMode: MatchMode?
    var date: Date = Date()
    
    var posTags: [Tag] = []
    var negTags: [Tag] = []
    
    init(id: Int, name : String) {
        self.id = id
        self.name = name
        self.date = Date()
    }
    
    var posTagKeys : [String] {
        return posTags.map({ (tag) in
            return tag.key
        })
    }
    
    var negTagKeys : [String] {
        return negTags.map({ (tag) in
            return tag.key
        })
    }
    
    var posTagEffectiveKeys : [String] {
        return posTags.map({ (tag) in
            return tag.effectiveKey
        })
    }
    
    var negTagEffectiveKeys : [String] {
        return negTags.map({ (tag) in
            return tag.effectiveKey
        })
    }
    
    var effectiveMatchMode: MatchMode {
        var matchMode = self.matchMode ?? UserData.instance.matchMode
        if Settings.instance.disableSettingsMatchMode && Settings.instance.settingsMatchMode != nil {
            matchMode = Settings.instance.settingsMatchMode!
        }
        return matchMode
    }
    
    func touch(completion : (() -> ())? = nil) {
        self.date = Date()
        UserData.instance.touch(error: nil, completion: completion)
    }
    
    var description: String {
        return name
    }
    
    func toJSON() -> JSON {
        var raw: [String:Any] = [:]
        raw["i"] = id
        raw["n"] = name
        raw["r"] = relationType.rawValue
        raw["m"] = matchMode?.rawValue ?? -1
        raw["d"] = date.iso
        raw["pt"] = posTagKeys
        raw["nt"] = negTagKeys
        return JSON(raw)
    }
    
    func toJSONString() -> String {
        return toJSON().rawString(String.Encoding.utf8, options: JSONSerialization.WritingOptions(rawValue: 0))!
    }
    
    static func fromJSON(json : JSON) -> Profile {
        let raw = json.rawValue as! [String: Any]
        let profile = Profile(id: raw["i"] as! Int,
                              name: raw["n"] as! String)
        profile.relationType = RelationType(rawValue: raw["r"] as? String ?? RelationType.none.rawValue)!
        let matchModeInt = raw["m"] as? Int ?? -1
        if matchModeInt >= 0 {
            profile.matchMode = MatchMode(rawValue: matchModeInt)!
        }
        profile.date = (raw["d"] as! String).dateFromISO!
        
        for posTagKey in json["pt"] {
            if let tag = Tag.lookupKey(posTagKey.1.rawValue as! String) {
                profile.posTags.append(tag)
            }
        }
        
        for negTagKey in json["nt"] {
            if let tag = Tag.lookupKey(negTagKey.1.rawValue as! String) {
                profile.negTags.append(tag)
            }
        }
        
        return profile
    }
    
    static func fromJSONString(sJSON : String) -> Profile? {
        if let data = sJSON.data(using: String.Encoding.utf8, allowLossyConversion: false) {
            return Profile.fromJSON(json: JSON(data: data))
        }
        return nil
    }
}

func ==(lhs: Profile, rhs: Profile) -> Bool {
    return lhs.id == rhs.id
}
