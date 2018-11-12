//
//  TagQuery.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 19.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

struct TagQuery {
    
    var favorite: Bool = false
    var search: Bool = false
    var searchText: String = ""
    var own: Bool = false
    var categoryKey: String = ""
    var categoryStaged: Bool = false
    var name: String = ""
    var langCode: String = ""
}
