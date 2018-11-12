//
//  StageTag.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 23.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

struct StageTag {
    var key: String = ""
    var langCode: String = ""
    var name: String = ""
    var categoryKey: String = ""
    var categoryName: String = ""
    var primaryLangKey: String = ""
    var counter: Int = 1
    
    var tag: Tag {
        let tag = Tag(key: key,
                      langCode: langCode,
                      name: name,
                      categoryKey: categoryKey,
                      primaryLangKey : primaryLangKey)
        tag.stage = true
        return tag
    }
}
