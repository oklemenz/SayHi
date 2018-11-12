//
//  NewTag.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 19.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

class NewTag {

    var assignPos: Bool = true
    
    var key: String = ""
    var langCode: String = ""
    var _name: String = ""
    var name: String {
        set {
            _name = newValue.capitalize
        }
        get { return _name }
    }
    var category: Category?
    var categoryNew: Bool = false
    var primaryLangTag: Tag?
    var primaryLangCategory: Category?
    
    var tag : Tag {
        let tag = Tag(key: key,
                      langCode: langCode,
                      name: name,
                      categoryKey: category!.key,
                      primaryLangKey: primaryLangTag?.key ?? "")
        if let category = self.category {
            tag._category = category
        }
        return tag
    }
}
