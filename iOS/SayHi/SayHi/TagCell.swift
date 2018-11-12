//
//  TagCell.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class TagCell: UICollectionViewCell {
    
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
        self.addSubview(tagView)
    }
    
    func setMaxWidthScreen() {
        self.tagView.setMaxWidthScreen()
    }
    
    func setMaxWidthHalfScreen() {
        self.tagView.setMaxWidthHalfScreen()
    }
    
    func setMaxWidthScreenWithBias(_ bias: CGFloat) {
        self.tagView.setMaxWidthScreenWithBias(bias)
    }
}
