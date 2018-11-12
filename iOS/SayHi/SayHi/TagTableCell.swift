//
//  TagTableCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 19.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class TagTableCell: UITableViewCell {
    
    @IBOutlet weak var containerView: UIView!
    
    var tagView : TagView!
    
    var _data : Tag!
    var data : Tag! {
        set {
            _data = newValue
            tagView.data = data
        }
        get {
            return _data
        }
    }
    
    override func awakeFromNib() {
        self.tagView = Bundle.main.loadNibNamed("TagView", owner: nil, options: nil)?.first as? TagView
        self.tagView.translatesAutoresizingMaskIntoConstraints = false
        self.containerView.addSubview(tagView)
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.tagView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .centerY,
            multiplier: 1.0,
            constant: 0))
        self.tagView!.addConstraint(NSLayoutConstraint(
            item: self.tagView!,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: 25))
        self.tagView!.addConstraint(NSLayoutConstraint(
            item: self.tagView!,
            attribute: .width,
            relatedBy: .lessThanOrEqual,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: TagMaxWidth - TagMaxWidthInset))
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.tagView!,
            attribute: .leadingMargin,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .leadingMargin,
            multiplier: 1.0,
            constant: 0))
        self.containerView.isUserInteractionEnabled = false 
    }
        
    override func setSelected(_ selected: Bool, animated: Bool) {
        let color = tagView.backgroundColor
        super.setSelected(selected, animated: animated)
        
        if selected {
            tagView.backgroundColor = color
        }
    }
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        let color = tagView.backgroundColor
        super.setHighlighted(highlighted, animated: animated)
        
        if highlighted {
            tagView.backgroundColor = color
        }
    }
}
