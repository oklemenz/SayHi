//
//  CategoryCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 13.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

@objc protocol CategoryCellDelegate {
    func didPressCategory(_ button : UIButton)
}

class CategoryCell: UICollectionViewCell, CategoryViewDelegate {
    
    weak var delegate: CategoryCellDelegate?
    
    func didPressCategory(_ button : UIButton) {
        self.delegate?.didPressCategory(button)
    }
    
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
        self.categoryView.delegate = self
        self.addSubview(categoryView)
    }
    
    func setMaxWidthScreen() {
        self.categoryView.setMaxWidthScreen()
    }
    
    func setMaxWidthHalfScreen() {
        self.categoryView.setMaxWidthHalfScreen()
    }
    
    func setMaxWidthScreenWithBias(_ bias: CGFloat) {
        self.categoryView.setMaxWidthScreenWithBias(bias)
    }
}
