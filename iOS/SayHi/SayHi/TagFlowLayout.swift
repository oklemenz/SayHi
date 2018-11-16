//
//  TagFlowLayout.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

extension UICollectionViewLayoutAttributes {
    func leftAlignFrameWithSectionInset(_ sectionInset:UIEdgeInsets){
        var frame = self.frame
        frame.origin.x = sectionInset.left
        self.frame = frame
    }
}

class TagFlowLayout: UICollectionViewFlowLayout {
    
    override func awakeFromNib() {
        self.sectionInset = UIEdgeInsets.init(top: 4, left: 4, bottom: 4, right: 4)
    }
    
    override func layoutAttributesForElements(in rect: CGRect) -> [UICollectionViewLayoutAttributes]? {
        
        var attributesCopy: [UICollectionViewLayoutAttributes] = []
        if let attributes = super.layoutAttributesForElements(in: rect) {
            attributes.forEach({ attributesCopy.append($0.copy() as! UICollectionViewLayoutAttributes) })
        }
        
        for attributes in attributesCopy {
            if attributes.representedElementKind == nil {
                let indexpath = attributes.indexPath
                if let attr = layoutAttributesForItem(at: indexpath) {
                    attributes.frame = attr.frame
                }
            }
        }
        return attributesCopy
    }
    
    override func layoutAttributesForItem(at indexPath: IndexPath) -> UICollectionViewLayoutAttributes? {
        if let currentItemAttributes = super.layoutAttributesForItem(at: indexPath)?.copy() as? UICollectionViewLayoutAttributes {
            let sectionInset = self.evaluatedSectionInsetForItemAtIndex((indexPath as NSIndexPath).section)
            let isFirstItemInSection = (indexPath as NSIndexPath).item == 0
            let layoutWidth = self.collectionView!.frame.width - sectionInset.left - sectionInset.right
            
            if isFirstItemInSection {
                currentItemAttributes.leftAlignFrameWithSectionInset(sectionInset)
                return currentItemAttributes
            }
            
            let previousIndexPath = IndexPath(item: (indexPath as NSIndexPath).item - 1, section: (indexPath as NSIndexPath).section)
            
            let previousFrame = layoutAttributesForItem(at: previousIndexPath)?.frame ?? CGRect.zero
            let previousFrameRightPoint = previousFrame.origin.x + previousFrame.width
            let currentFrame = currentItemAttributes.frame
            let strecthedCurrentFrame = CGRect(x: sectionInset.left,
                                               y: currentFrame.origin.y,
                                               width: layoutWidth,
                                               height: currentFrame.size.height)
            // if the current frame, once left aligned to the left and stretched to the full collection view
            // widht intersects the previous frame then they are on the same line
            let isFirstItemInRow = !previousFrame.intersects(strecthedCurrentFrame)
            
            if isFirstItemInRow {
                // make sure the first item on a line is left aligned
                currentItemAttributes.leftAlignFrameWithSectionInset(sectionInset)
                return currentItemAttributes
            }
            
            var frame = currentItemAttributes.frame
            frame.origin.x = previousFrameRightPoint + evaluatedMinimumInteritemSpacingForSectionAtIndex((indexPath as NSIndexPath).section)
            currentItemAttributes.frame = frame
            return currentItemAttributes
            
        }
        return nil
    }
    
    func evaluatedMinimumInteritemSpacingForSectionAtIndex(_ sectionIndex:Int) -> CGFloat {
        if let delegate = self.collectionView?.delegate as? UICollectionViewDelegateFlowLayout {
            if delegate.responds(to: #selector(UICollectionViewDelegateFlowLayout.collectionView(_:layout:minimumInteritemSpacingForSectionAt:))) {
                return delegate.collectionView!(self.collectionView!, layout: self, minimumInteritemSpacingForSectionAt: sectionIndex)
                
            }
        }
        return self.minimumInteritemSpacing
        
    }
    
    func evaluatedSectionInsetForItemAtIndex(_ index: Int) ->UIEdgeInsets {
        if let delegate = self.collectionView?.delegate as? UICollectionViewDelegateFlowLayout {
            if  delegate.responds(to: #selector(UICollectionViewDelegateFlowLayout.collectionView(_:layout:insetForSectionAt:))) {
                return delegate.collectionView!(self.collectionView!, layout: self, insetForSectionAt: index)
            }
        }
        return self.sectionInset
    }
}
