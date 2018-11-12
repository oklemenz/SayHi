//
//  NewCategory.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 19.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

class NewCategory {
    
    var key: String = ""
    var langCode: String = ""
    var _name: String = ""
    var name: String {
        set {
            _name = newValue.capitalize
        }
        get { return _name }
    }
    var primaryLangCategory: Category?
    
    var category: Category {
        return Category(key: key,
                        langCode: langCode,
                        name: name,
                        color: CategoryStagedColor,
                        icon: CategoryStagedIcon,
                        order: 0,
                        primaryLangKey: primaryLangCategory?.key ?? "",
                        refKey: nil,
                        refPrimaryLangKey: nil)
    }
}
