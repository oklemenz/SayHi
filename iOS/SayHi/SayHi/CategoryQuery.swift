//
//  CategoryQuery.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 20.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

struct CategoryQuery {
    var favorite: Bool = false    
    var search: Bool = false
    var searchText: String = ""
    var name: String = ""
    var langCode: String = ""
    var primaryLangKey: String = ""
}
