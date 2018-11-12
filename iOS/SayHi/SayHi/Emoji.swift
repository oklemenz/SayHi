//
//  Emoji.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 18.04.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation

class Emoji {
    
    static var matchMode: String {
        return "🔳"
    }
    
    static var relationType: String {
        return "🔗"
    }
    
    static var like: String {
        if let leftLabel = Settings.instance.leftLabel {
            return leftLabel
        }
        return "👍🏼"
    }
    
    static var dislike: String {
        if let rightLabel = Settings.instance.rightLabel {
            return rightLabel
        }
        return "👎🏼"
    }
}
