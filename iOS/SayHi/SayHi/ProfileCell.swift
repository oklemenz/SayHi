//
//  ProfileCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 09.01.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class ProfileCell: UITableViewCell {
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        self.imageView?.contentMode = .center
        self.imageView?.frame = CGRect(x: 4, y: 0, width: 42 + 2 * 4, height: 42)
    }
}
