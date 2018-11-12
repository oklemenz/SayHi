//
//  Tag.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation
import SwiftyJSON

let TagStagedIcon = "tag"

class Tag: Equatable {

    var key: String = ""
    var langCode: String = ""
    var _name: String = ""
    var name: String {
        set {
            _name = newValue
            search = name.searchNormalized(langCode: langCode)
        }
        get {
            return _name
        }
    }
    
    var _categoryKey: String = ""
    var categoryKey : String {
        set {
            _categoryKey = newValue
            _category = nil
        }
        get {
            return _categoryKey
        }
    }
    
    var primaryLangKey: String = ""
    var refKey: String?
    var _refPrimaryLangKey: String?
    var refPrimaryLangKey: String? {
        set { _refPrimaryLangKey = newValue }
        get { return _refPrimaryLangKey ?? refKey }
    }

    var search: String = ""
    var favorite: Bool = false
    var selected: Bool = false
    var stage: Bool = false
    
    var space: String = ""
    
    init(key: String, langCode: String, name: String, categoryKey: String, primaryLangKey: String, refKey: String? = nil, refPrimaryLangKey: String? = nil,
         favorite: Bool = false) {
        self.key = key
        self.langCode = langCode
        self.name = name
        self.categoryKey = categoryKey
        self.primaryLangKey = primaryLangKey
        if let refKey = refKey {
            self.refKey = refKey
        }
        if let refPrimaryLangKey = refPrimaryLangKey {
            self.refPrimaryLangKey = refPrimaryLangKey
        }
        self.favorite = favorite
    }
    
    var _category: Category?
    var category: Category? {
        if !categoryKey.isEmpty {
            if _category == nil {
                _category = Cache.instance.lookupCategory(key: categoryKey)
            }
            return _category
        }
        return nil
    }

    var _primaryLangTag: Tag?
    var primaryLangTag: Tag? {
        if !primaryLangKey.isEmpty {
            if _primaryLangTag == nil {
               _primaryLangTag = Cache.instance.lookupTag(key: primaryLangKey)
            }
            return _primaryLangTag
        }
        return nil
    }
    
    var effectiveKey: String {
        get {
            if let refPrimaryLangKey = refPrimaryLangKey {
                return refPrimaryLangKey
            }
            return !primaryLangKey.isEmpty ? primaryLangKey : key
        }
    }
    
    var description : String {
        return name
    }
    
    func toJSON() -> JSON {
        var raw: [String:Any] = [:]
        raw["k"] = key
        raw["l"] = langCode
        raw["n"] = name
        raw["c"] = categoryKey
        raw["p"] = primaryLangKey
        raw["r"] = refKey ?? ""
        raw["rp"] = refPrimaryLangKey ?? ""
        raw["s"] = space
        return JSON(raw)
    }
    
    func toJSONString() -> String {
        return toJSON().rawString(String.Encoding.utf8, options: JSONSerialization.WritingOptions(rawValue: 0))!
    }
    
    static func fromJSON(json : JSON) -> Tag {
        var raw = json.rawValue as! [String:Any]
        let tag = Tag(key: raw["k"] as! String,
                      langCode: raw["l"] as! String,
                      name: raw["n"] as! String,
                      categoryKey: raw["c"] as! String,
                      primaryLangKey: raw["p"] as! String,
                      refKey: !(raw["r"] as! String).isEmpty ? raw["r"] as? String : nil,
                      refPrimaryLangKey: !(raw["rp"] as! String).isEmpty ? raw["rp"] as? String : nil)
        tag.space = raw["s"] as! String
        return tag
    }
    
    static func fromJSONString(jsonString : String) -> Tag? {
        if let data = jsonString.data(using: String.Encoding.utf8, allowLossyConversion: false) {
            return Tag.fromJSON(json: JSON(data: data))
        }
        return nil
    }
    
    static func lookupKey(_ key: String) -> Tag? {
        if let tag = Cache.instance.lookupTag(key: key) {
            return tag
        }
        if let tag = UserData.instance.ownTags[key] {
            return tag
        }
        return nil
    }
}

func ==(lhs: Tag, rhs: Tag) -> Bool {
    return lhs.key == rhs.key
}
