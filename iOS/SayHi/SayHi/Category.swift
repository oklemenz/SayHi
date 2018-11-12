//
//  Category.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 13.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation
import SwiftyJSON

let CategoryStagedIcon = "staged"
let CategoryStagedColor = "#999999"

let CategoryFavorite = Category(mark: 1, icon: "favorite")
let CategorySearch = Category(mark: 2, icon: "search")
let CategoryOwn = Category(mark: 3, icon: "own")
let CategoryStaged = Category(mark: 4, icon: "staged")
let CategoryMore = Category(mark: 5, icon: "more")

class Category: Equatable {
        
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
    var _color: String = ""
    var color: String {
        set {
            _color = newValue
            if !color.isEmpty {
                self.bgColor = UIColor.colorWithHexString(hexString: color)
                self.textColor = self.bgColor.isLight() ? UIColor.black : UIColor.white
            } else {
                self.bgColor = UIColor.clear
                self.textColor = UIColor.white
            }
        }
        get {
            return _color
        }
    }
    var _icon: String = ""
    var icon: String {
        set {
            _icon = newValue
            if !self.icon.isEmpty {
                self.iconImage = IconService.instance.icon(self.icon)
            }
        }
        get {
            return _icon
        }
    }
    var order: Int = 0
    
    var primaryLangKey: String = ""
    var refKey: String?
    var _refPrimaryLangKey: String?
    var refPrimaryLangKey: String? {
        set { _refPrimaryLangKey = newValue }
        get { return _refPrimaryLangKey ?? refKey }
    }

    var bgColor: UIColor = UIColor.white
    var textColor: UIColor = UIColor.black
    var iconImage: UIImage?

    var search: String = ""
    var favorite: Bool = false
    var selected: Bool = false
    var stage: Bool = false
    var mark: Int = 0
    
    var space: String = ""
    
    init(key: String, langCode: String, name: String,  color: String, icon: String, order: Int, primaryLangKey: String, refKey: String? = nil, refPrimaryLangKey: String? = nil, favorite: Bool = false) {
        self.key = key
        self.langCode = langCode
        self.name = name
        self.color = color
        self.icon = icon
        self.order = order
        self.primaryLangKey = primaryLangKey
        if let refKey = refKey {
            self.refKey = refKey
        }
        if let refPrimaryLangKey = refPrimaryLangKey {
            self.refPrimaryLangKey = refPrimaryLangKey
        }
        self.favorite = favorite
    }
    
    convenience init(mark: Int, icon: String) {
        self.init(key: "", langCode: "", name: "", color: "", icon: icon, order: 0, primaryLangKey: "")
        self.mark = mark
    }
    
    var _primaryLangCategory: Category?
    var primaryLangCategory: Category? {
        if !primaryLangKey.isEmpty {
            if _primaryLangCategory == nil {
                _primaryLangCategory = Cache.instance.lookupCategory(key: primaryLangKey)
            }
            return _primaryLangCategory
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
    
    var description: String {
        return name
    }
    
    func deriveFrom(_ category: Category) {
        self.color = category.color
        self.icon = category.icon
    }
    
    func toJSON() -> JSON {
        var raw: [String:Any] = [:]
        raw["k"] = key
        raw["l"] = langCode
        raw["n"] = name
        raw["c"] = color
        raw["i"] = icon
        raw["o"] = order
        raw["p"] = primaryLangKey
        raw["r"] = refKey ?? ""
        raw["rp"] = refPrimaryLangKey ?? ""
        raw["s"] = space
        return JSON(raw)
    }
    
    func toJSONString() -> String {
        return toJSON().rawString(String.Encoding.utf8, options: JSONSerialization.WritingOptions(rawValue: 0))!
    }
    
    static func fromJSON(json : JSON) -> Category {
        var raw = json.rawValue as! [String:Any]
        let category = Category(key: raw["k"] as! String,
                                langCode: raw["l"] as! String,
                                name: raw["n"] as! String,
                                color: raw["c"] as! String,
                                icon: raw["i"] as! String,
                                order: raw["o"] as! Int,
                                primaryLangKey : raw["p"] as! String,
                                refKey: !(raw["r"] as! String).isEmpty ? raw["r"] as? String : nil,
                                refPrimaryLangKey: !(raw["rp"] as! String).isEmpty ? raw["rp"] as? String : nil)
        category.space = raw["s"] as! String
        return category
    }
    
    static func fromJSONString(jsonString : String) -> Category? {
        if let data = jsonString.data(using: String.Encoding.utf8, allowLossyConversion: false) {
            return Category.fromJSON(json: JSON(data: data))
        }
        return nil
    }
}

func ==(lhs: Category, rhs: Category) -> Bool {
    return lhs.key == rhs.key && lhs.mark == rhs.mark
}
