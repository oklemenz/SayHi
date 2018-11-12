//
//  StageCategory.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 23.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation

struct StageCategory {
    var key: String = ""
    var langCode: String = ""
    var name: String = ""
    var primaryLangKey: String = ""
    var counter: Int = 1
    
    var category: Category {
        let category = Category(key: key,
                                langCode: langCode,
                                name: name,
                                color: CategoryStagedColor,
                                icon: CategoryStagedIcon,
                                order: 0,
                                primaryLangKey: primaryLangKey)
        category.stage = true
        return category
    }
}
