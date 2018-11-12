//
//  SimilarCategoryCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 01.02.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class SimilarCategoryTableCell: UITableViewCell {
    
    @IBOutlet weak var containerView: UIView!
    
    var categoryView : CategoryView!
    var primaryLangCategoryView : CategoryView!
    var primaryLangCategoryLabel : UILabel!
        
    var _data : Category!
    var data : Category! {
        set {
            _data = newValue
            categoryView.data = data
            if let primaryLangCategory = data.primaryLangCategory {
                primaryLangCategoryView.data = primaryLangCategory
                self.categoryView!.addConstraint(NSLayoutConstraint(
                    item: self.categoryView!,
                    attribute: .width,
                    relatedBy: .lessThanOrEqual,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1.0,
                    constant: CategoryMaxHalfWidth - CategoryMaxWidthInset))
                self.primaryLangCategoryView!.addConstraint(NSLayoutConstraint(
                    item: self.primaryLangCategoryView!,
                    attribute: .width,
                    relatedBy: .lessThanOrEqual,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1.0,
                    constant: CategoryMaxHalfWidth - CategoryMaxWidthInset))
            } else {
                primaryLangCategoryView.isHidden = true
                primaryLangCategoryLabel.isHidden = true
                self.categoryView!.addConstraint(NSLayoutConstraint(
                    item: self.categoryView!,
                    attribute: .width,
                    relatedBy: .lessThanOrEqual,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1.0,
                    constant: CategoryMaxWidth - CategoryMaxWidthInset))
            }
        }
        get {
            return _data
        }
    }
    
    override func awakeFromNib() {
        self.categoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
        self.categoryView.translatesAutoresizingMaskIntoConstraints = false
        self.containerView.addSubview(self.categoryView)
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
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .leadingMargin,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .leadingMargin,
            multiplier: 1.0,
            constant: 0))

        self.primaryLangCategoryLabel = UILabel()
        self.primaryLangCategoryLabel.font = UIFont.systemFont(ofSize: 14.0)
        self.primaryLangCategoryLabel.textColor = AccentColor
        self.primaryLangCategoryLabel.translatesAutoresizingMaskIntoConstraints = false
        self.primaryLangCategoryLabel.text = "EN:".localized
        self.containerView.addSubview(self.primaryLangCategoryLabel)
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryLabel!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .centerY,
            multiplier: 1.0,
            constant: 0))
        self.primaryLangCategoryLabel!.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryLabel!,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: 25))
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryLabel!,
            attribute: .leading,
            relatedBy: .equal,
            toItem: self.categoryView,
            attribute: .trailing,
            multiplier: 1.0,
            constant: 10))
        
        self.primaryLangCategoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
        self.primaryLangCategoryView.translatesAutoresizingMaskIntoConstraints = false
        self.containerView.addSubview(self.primaryLangCategoryView)
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.containerView,
            attribute: .centerY,
            multiplier: 1.0,
            constant: 0))
        self.primaryLangCategoryView!.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryView!,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: 25))
        self.containerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryView!,
            attribute: .leading,
            relatedBy: .equal,
            toItem: self.primaryLangCategoryLabel,
            attribute: .trailing,
            multiplier: 1.0,
            constant: 10))
        
        self.containerView.isUserInteractionEnabled = false
    }
    
    override func setSelected(_ selected: Bool, animated: Bool) {
        let color1 = categoryView.backgroundColor
        let color2 = primaryLangCategoryView.backgroundColor
        super.setSelected(selected, animated: animated)
        
        if selected {
            categoryView.backgroundColor = color1
            primaryLangCategoryView.backgroundColor = color2
        }
    }
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        let color1 = categoryView.backgroundColor
        let color2 = primaryLangCategoryView.backgroundColor
        super.setHighlighted(highlighted, animated: animated)
        
        if highlighted {
            categoryView.backgroundColor = color1
            primaryLangCategoryView.backgroundColor = color2
        }
    }
}
