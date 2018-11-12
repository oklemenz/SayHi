//
//  Enum.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 07.04.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation

enum Gender : String {
    case none   = ""
    case male   = "m"
    case female = "f"
    
    static let list : [Gender] = [.none, .male, .female]
}

enum MatchMode : Int {
    case basic = 1
    case exact = 2
    case adapt = 3
    case tries = 4
    case open  = 5
    
    var description: String {
        switch self {
        case .basic:
            return "basic"
        case .exact:
            return "exact"
        case .adapt:
            return "adapt"
        case .tries:
            return "tries"
        case .open:
            return "open"
        }
    }
    
    var externalDescription: String {
        switch self {
        case .basic:
            return "basic"
        case .exact:
            return "exact"
        case .adapt:
            return "adapt"
        case .tries:
            return "try"
        case .open:
            return "open"
        }
    }
    
    var numberedDescription: String {
        return "\(description)_\(rawValue)"
    }
    
    static func fromDescription(_ description: String) -> MatchMode? {
        if description == "basic" {
            return .basic
        } else if description == "exact" {
            return .exact
        } else if description == "adapt" {
            return .adapt
        } else if description == "tries" {
            return .tries
        } else if description == "open" {
            return .open
        }
        return nil
    }
    
    static let list : [MatchMode] = [.basic, .exact, .adapt, .tries, .open]
}

enum PasscodeTimeout : Int {
    case min0  = 0
    case min1  = 1
    case min5  = 5
    case min10 = 10
    
    static let list : [PasscodeTimeout] = [.min0, .min1, .min5, .min10]
}

enum RelationType : String {
    
    case none = "None"
    case date = "Date"
    case single = "Single"
    case family = "Family"
    case partner = "Partner"
    case wife = "Wife"
    case husband = "Husband"
    case parent = "Parent"
    case father = "Father"
    case mother = "Mother"
    case child = "Child"
    case son = "Son"
    case daugther = "Daugther"
    case sibling = "Sibling"
    case brother = "Brother"
    case sister = "Sister"
    case grandparent = "Grandparent"
    case grandfather = "Grandfather"
    case grandmother = "Grandmother"
    case grandchild = "Grandchild"
    case grandson = "Grandson"
    case granddaughter = "Granddaughter"
    case relative = "Relative"
    case uncle = "Uncle"
    case aunt = "Aunt"
    case cousins = "Cousins"
    case cousin = "Cousin (m)"
    case cousine = "Cousin (f)"
    case nephew = "Nephew"
    case niece = "Niece"
    case friend = "Friend"
    case boyfriend = "Boyfriend"
    case girlfriend = "Girlfriend"
    case colleague = "Colleague"
    case boss = "Boss"
    case manager = "Manager"
    case employee = "Employee"
    case peer = "Peer"
    case other = "Other"
    
    static let list : [RelationType] = [.none, .date, .single,
                                        .boyfriend, .girlfriend, .wife, .husband,
                                        .partner, .family, .friend,
                                        .child, .son, .daugther,
                                        .parent, .father, .mother,
                                        .sibling, .brother, .sister,
                                        .relative, .uncle, .aunt,
                                        .grandchild, .grandson, .granddaughter,
                                        .grandparent, .grandfather, .grandmother,
                                        .cousins, .cousin, .cousine,
                                        .nephew, .niece,
                                        .colleague, .boss, .manager, .employee, .peer,
                                        .other]
}
