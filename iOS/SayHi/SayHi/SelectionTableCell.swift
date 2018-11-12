//
//  SelectionTableCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 21.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class SelectionTableCell: UITableViewCell {

    override func setSelected(_ selected: Bool, animated: Bool) {
        var color : [UIView:UIColor?] = [:]
        for view in self.contentView.subviews {
            color[view] = view.backgroundColor
        }
        super.setSelected(selected, animated: animated)
        
        if selected {
            for (view, color) in color {
                view.backgroundColor = color
            }
        }
    }
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        var color : [UIView:UIColor?] = [:]
        for view in self.contentView.subviews {
            color[view] = view.backgroundColor
        }
        super.setHighlighted(highlighted, animated: animated)
        
        if highlighted {
            for (view, color) in color {
                view.backgroundColor = color
            }
        }
    }
}
