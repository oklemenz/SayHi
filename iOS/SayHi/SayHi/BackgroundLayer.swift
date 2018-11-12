//
//  BackLayer.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 23.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class BackgroundLayer : CAGradientLayer {
    
    override init() {
        super.init()
        refresh()
        
    }
    
    override init(layer: Any) {
        super.init(layer: layer)
        refresh()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        refresh()
    }
    
    func refresh() {
        self.colors = [GradientColor1.cgColor, GradientColor2.cgColor]
        self.startPoint = CGPoint(x: 0.25, y: 0.0)
        self.endPoint = CGPoint(x: 0.75, y: 1.0)
        self.zPosition = -1
    }
}
