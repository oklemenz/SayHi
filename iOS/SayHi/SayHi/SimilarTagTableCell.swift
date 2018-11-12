//
//  SimilarCategoryCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 01.02.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class SimilarTagTableCell: UITableViewCell {
    
    @IBOutlet weak var tagContainerView: UIView!
    @IBOutlet weak var categoryContainerView: UIView!
    
    var tagView : TagView!
    var primaryLangTagView : TagView!
    var primaryLangTagLabel : UILabel!
    
    var categoryView : CategoryView!
    var primaryLangCategoryView : CategoryView!
    var primaryLangCategoryLabel : UILabel!
    
    var _data : Tag!
    var data : Tag! {
        set {
            _data = newValue
            tagView.data = data
            if let primaryLangTag = data.primaryLangTag {
                primaryLangTagView.data = primaryLangTag
                self.tagView!.addConstraint(NSLayoutConstraint(
                    item: self.tagView!,
                    attribute: .width,
                    relatedBy: .lessThanOrEqual,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1.0,
                    constant: TagMaxHalfWidth - TagMaxWidthInset))
                self.primaryLangCategoryView!.addConstraint(NSLayoutConstraint(
                    item: self.primaryLangCategoryView!,
                    attribute: .width,
                    relatedBy: .lessThanOrEqual,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1.0,
                    constant: TagMaxHalfWidth - TagMaxWidthInset))
            } else {
                primaryLangTagView.isHidden = true
                primaryLangTagLabel.isHidden = true
                self.tagView!.addConstraint(NSLayoutConstraint(
                    item: self.tagView!,
                    attribute: .width,
                    relatedBy: .lessThanOrEqual,
                    toItem: nil,
                    attribute: .notAnAttribute,
                    multiplier: 1.0,
                    constant: TagMaxWidth - TagMaxWidthInset))
            }
            if let category = data.category {
                categoryView.data = category
                if let primaryLangCategory = category.primaryLangCategory {
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
            } else {
                categoryView.isHidden = true
                primaryLangCategoryView.isHidden = true
                primaryLangCategoryLabel.isHidden = true
            }
        }
        get {
            return _data
        }
    }
    
    override func awakeFromNib() {
        self.tagView = Bundle.main.loadNibNamed("TagView", owner: nil, options: nil)?.first as? TagView
        self.tagView.translatesAutoresizingMaskIntoConstraints = false
        self.tagContainerView.addSubview(self.tagView)
        self.tagContainerView.addConstraint(NSLayoutConstraint(
            item: self.tagView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.tagContainerView,
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
        self.tagContainerView.addConstraint(NSLayoutConstraint(
            item: self.tagView!,
            attribute: .leadingMargin,
            relatedBy: .equal,
            toItem: self.tagContainerView,
            attribute: .leadingMargin,
            multiplier: 1.0,
            constant: 0))
        
        self.primaryLangTagLabel = UILabel()
        self.primaryLangTagLabel.font = UIFont.systemFont(ofSize: 14.0)
        self.primaryLangTagLabel.textColor = AccentColor
        self.primaryLangTagLabel.translatesAutoresizingMaskIntoConstraints = false
        self.primaryLangTagLabel.text = "EN:".localized
        self.tagContainerView.addSubview(self.primaryLangTagLabel)
        self.tagContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangTagLabel!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.tagContainerView,
            attribute: .centerY,
            multiplier: 1.0,
            constant: 0))
        self.primaryLangTagLabel!.addConstraint(NSLayoutConstraint(
            item: self.primaryLangTagLabel!,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: 25))
        self.tagContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangTagLabel!,
            attribute: .leading,
            relatedBy: .equal,
            toItem: self.tagView,
            attribute: .trailing,
            multiplier: 1.0,
            constant: 10))
        
        self.primaryLangTagView = Bundle.main.loadNibNamed("TagView", owner: nil, options: nil)?.first as? TagView
        self.primaryLangTagView.translatesAutoresizingMaskIntoConstraints = false
        self.tagContainerView.addSubview(self.primaryLangTagView)
        self.tagContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangTagView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.tagContainerView,
            attribute: .centerY,
            multiplier: 1.0,
            constant: 0))
        self.primaryLangTagView!.addConstraint(NSLayoutConstraint(
            item: self.primaryLangTagView!,
            attribute: .height,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: 25))
        self.tagContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangTagView!,
            attribute: .leading,
            relatedBy: .equal,
            toItem: self.primaryLangTagLabel,
            attribute: .trailing,
            multiplier: 1.0,
            constant: 10))
        
        self.tagContainerView.isUserInteractionEnabled = false
        
        self.categoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
        self.categoryView.translatesAutoresizingMaskIntoConstraints = false
        self.categoryContainerView.addSubview(self.categoryView)
        self.categoryContainerView.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.categoryContainerView,
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
        self.categoryContainerView.addConstraint(NSLayoutConstraint(
            item: self.categoryView!,
            attribute: .leadingMargin,
            relatedBy: .equal,
            toItem: self.categoryContainerView,
            attribute: .leadingMargin,
            multiplier: 1.0,
            constant: 0))
        
        self.primaryLangCategoryLabel = UILabel()
        self.primaryLangCategoryLabel.font = UIFont.systemFont(ofSize: 14.0)
        self.primaryLangCategoryLabel.textColor = AccentColor
        self.primaryLangCategoryLabel.translatesAutoresizingMaskIntoConstraints = false
        self.primaryLangCategoryLabel.text = "EN:".localized
        self.categoryContainerView.addSubview(self.primaryLangCategoryLabel)
        self.categoryContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryLabel!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.categoryContainerView,
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
        self.categoryContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryLabel!,
            attribute: .leading,
            relatedBy: .equal,
            toItem: self.categoryView,
            attribute: .trailing,
            multiplier: 1.0,
            constant: 10))
        
        self.primaryLangCategoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
        self.primaryLangCategoryView.translatesAutoresizingMaskIntoConstraints = false
        self.categoryContainerView.addSubview(self.primaryLangCategoryView)
        self.categoryContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryView!,
            attribute: .centerY,
            relatedBy: .equal,
            toItem: self.categoryContainerView,
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
        self.categoryContainerView.addConstraint(NSLayoutConstraint(
            item: self.primaryLangCategoryView!,
            attribute: .leading,
            relatedBy: .equal,
            toItem: self.primaryLangCategoryLabel,
            attribute: .trailing,
            multiplier: 1.0,
            constant: 10))
        
        self.categoryContainerView.isUserInteractionEnabled = false
    }
    
    override func setSelected(_ selected: Bool, animated: Bool) {
        let color1 = tagView.backgroundColor
        let color2 = primaryLangTagView.backgroundColor
        let color3 = categoryView.backgroundColor
        let color4 = primaryLangCategoryView.backgroundColor
        super.setSelected(selected, animated: animated)
        
        if selected {
            tagView.backgroundColor = color1
            primaryLangTagView.backgroundColor = color2
            categoryView.backgroundColor = color3
            primaryLangCategoryView.backgroundColor = color4
        }
    }
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        let color1 = tagView.backgroundColor
        let color2 = primaryLangTagView.backgroundColor
        let color3 = categoryView.backgroundColor
        let color4 = primaryLangCategoryView.backgroundColor
        super.setHighlighted(highlighted, animated: animated)
        
        if highlighted {
            tagView.backgroundColor = color1
            primaryLangTagView.backgroundColor = color2
            categoryView.backgroundColor = color3
            primaryLangCategoryView.backgroundColor = color4
        }
    }
}
