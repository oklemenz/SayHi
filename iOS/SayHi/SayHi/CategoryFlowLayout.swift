//
//  CategoryFlowLayout.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 13.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class CategoryFlowLayout: UICollectionViewFlowLayout {
    
    override func awakeFromNib() {
        self.sectionInset = UIEdgeInsetsMake(4, 4, 4, 4)
    }
}
