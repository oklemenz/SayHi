//
//  CategoryTableCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 19.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class CategoryTableCell: UITableViewCell {
    
    @IBOutlet weak var containerView: UIView!
    var categoryView : CategoryView!
    
    var _data : Category!
    var data : Category! {
        set {
            _data = newValue
            categoryView.data = data
        }
        get {
            return _data
        }
    }
    
    override func awakeFromNib() {
        self.categoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
        self.categoryView.translatesAutoresizingMaskIntoConstraints = false
        self.containerView.addSubview(categoryView)
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .centerY,
            multiplier: 1.0,
            constant: 0))
        self.categoryView!.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: 25))
        self.categoryView!.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .width,
            relatedBy: .lessThanOrEqual,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: CategoryMaxWidth - CategoryMaxWidthInset))
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .leadingMargin,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .leadingMargin,
            multiplier: 1.0,
            constant: 0))
        self.containerView.isUserInteractionEnabled = false
    }
    
    override func setSelected(_ selected: Bool, animated: Bool) {
        let color = categoryView.backgroundColor
        super.setSelected(selected, animated: animated)
        
        if selected {
            categoryView.backgroundColor = color
        }
    }
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        let color = categoryView.backgroundColor
        super.setHighlighted(highlighted, animated: animated)
        
        if highlighted {
            categoryView.backgroundColor = color
        }
    }
}
