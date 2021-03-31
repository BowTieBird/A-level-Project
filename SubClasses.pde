class LRArrowBtn extends Button {
  boolean right = true;

  LRArrowBtn(boolean _right) { right = _right; } 

  void show() {
    super.show();
    arrow();
  }

  void arrow() {
    fill(fg);
    float l = w*sqrt(2)/4;
    if (right) triangle(x+l, y+h/4, x+w*3/4, y+h/2, x+l, y+h*3/4);
    else triangle(x+w-l, y+h/4, x+w/4, y+h/2, x+w-l, y+h*3/4);
  }
  
  void flip() { right ^= true; }
}

class BinBtn extends Button {
  void show() {
    super.show();
    drawBin();
  }
  
  void drawBin() {
    fill(fg);
    rect(x + w/3, y + h/2, w/3, h/3);
    rect(x + 3*w/12, y + h/3, w/2, h/12);
    rect(x + w/3, y + 3*h/12, w/3, h/12 + 1);
  }
}

class PauseBtn extends Button {
  void show() {
    super.show();
    pause();
  }
  
  void pause() {
    fill(fg);
    rect(cx+0.05*w, y+0.25*h,0.2*w,0.5*h);
    rect(cx-0.05*w, y+0.25*h,-0.2*w,0.5*h);
  }
}

class UndoBtn extends Button {
  void show() {
    super.show();
    arrow();
  }
  
  void arrow() {
    ellipseMode(RADIUS);
    fill(fg);
    float min = min(w, h);
    float r1 = 3*min/10;
    ellipse(cx, cy, r1, r1);
    
    fill(bg);
    float r2 = 2*min/10;
    ellipse(cx, cy, r2, r2);
    rect(x, y, w/2, h/2);
    
    fill(fg);
    float d = (r1-r2)*2;
    triangle(cx, cy-r1-d/2, cx-min/2+d*sqrt(2), cy-(r1+r2)/2, cx, cy-r2+d/2);
    float r3 = (r1-r2)/2;
    ellipse(cx-r3-r2, cy, r3, r3);
  }
}

class SpeedBtn extends SliderBtn {  
  void show() {
    super.show();
    arrows();
  }
  
  void arrows() {
    fill(fg);
    float l = w*sqrt(2)/8;
    triangle(x+l, y+h/4, x+w/2, y+h/2, x+l, y+h*3/4);
    triangle(x+w/2, y+h/4, x+w-l, y+h/2, x+w/2, y+h*3/4);
  }
}

class PlusBtn extends Button {
  float plusWidth = 0.08;
  float plusHeight = 0.3;
  
  void show() {
    super.show();
    plus();
  }
  
  void plus() {   
    fill(fg);
    rect(cx-plusHeight*w, cy-plusWidth*h, 2*plusHeight*w, 2*plusWidth*h);
    rect(cx-plusWidth*w, cy-plusHeight*h, 2*plusWidth*w, 2*plusHeight*h);
  }
}

class SaveBtn extends Button {
  void show() {
    super.show();
    disk();
  }
  
  void disk() {
    fill(fg);
    float l = 0.1;
    rect(x+w*l, y+h*l, w*(1-2*l), h*(1-2*l));
    
    fill(bg);
    rect(x+w*2*l, y+h*1.5*l, w*(1-4*l), h*(0.5-1.5*l));
  }
}

class LoadBtn extends Button {
  void show() {
    super.show();
    folder();
  }
  
  void folder() {
    fill(fg);
    float tx = 0.6;
    float ty = 0.05;
    float lx = 0.4;
    float ly = 0.35;
    rect(cx-w*lx,cy-h*ly,2*w*lx,2*h*ly);
    fill(bg);
    rect(x+w*tx,cy-h*ly,w*(1-tx),2*h*ty);
  }
}

class QstnBtn extends Button {
  void show(){
    super.show();
    question();
  }
  
  void question() {
    ellipseMode(RADIUS);
    fill(fg);
    float min = min(w, h);
    float r1 = 2.5*min/10;
    float t = min/10;
    ellipse(cx, cy-t, r1, r1);
    
    fill(bg);
    float r2 = 1.5*min/10;
    ellipse(cx, cy-t, r2, r2);
    rect(x, cy-t, w/2, h/2);
    
    fill(fg);
    float r3 = (r1-r2)/2;
    ellipse(cx-2*t, cy-t, r3, r3);
    ellipse(cx, cy+t, r3, r3);
    float r4 = 0.8*min/10;
    ellipse(cx, cy+3*t, r4, r4);
  }
}

class SnapPoint extends Box {
  SnapPoint(Box parent) {
    setParent(parent);
  }
  
  float snapRadius = 30;
  
  boolean isNear(SnapPoint other) {
    float otherX = other.getCenterX();
    float otherY = other.getCenterY();
    float d = dist(cx, cy, otherX, otherY);
    return d <= snapRadius;
  }
}

class Connector extends SnapPoint {
  float connectorOffset = 20;
  float connectorWidth = 20;
  float connectorHeight = 10;
  
  Connector(Box parent) {
    super(parent);
    setBackground(parent.getBackground());
    setSize(connectorWidth, connectorHeight);
  }
}

class Slot extends Connector {
  Box left = new Box();
  Box right = new Box();
  
  Slot(Block parent) {
    super(parent);
    setOffset(connectorOffset, -connectorHeight);
    
    left.setParent(this);
    left.setBackground(bg);
    left.setOffsetX(-connectorOffset);
    
    right.setParent(this);
    right.setBackground(bg);
    right.setOffsetX(connectorWidth);
    
    updateWidth();
  }
  
  void updateWidth() {
    left.setSize(connectorOffset, connectorHeight);
    right.setSize(parent.getWidth() - connectorOffset - connectorWidth, connectorHeight);
  }
  
  void show() {
    left.show();
    right.show();
    
    if (cheekyDevMode) {
      if (mouseOver()) devDraw(this);
      if (mouseOver()) devSnap(this);
    }
  }
  
  void updatePos() {
    super.updatePos();
    left.updatePos();
    right.updatePos();
  }
  
  boolean mouseOver() { return (left.mouseOver() || right.mouseOver()); }
}

class Tab extends Connector {
  Tab(Block parent) {
    super(parent);
    setOffset(connectorOffset, parent.getHeight());
  }
  
  void show() {
    super.show();  
    if (cheekyDevMode) if (mouseOver()) devSnap(this);
  }
}

class Notch extends SnapPoint {
  float r = 10;
  float notchWidth = 20;
  float plusWidth = 0.7;
  float plusHeight = 0.15;
  
  color bg = color(0, 200, 0);
  color plusColor = color(255);
  
  Notch(Block parent) {
    super(parent);
    setBackground(bg);
    setSize(notchWidth, parent.getTotalHeight());
    setOffset(-notchWidth/2,-parent.getSlot().getHeight());
  }
  
  void show() {
    fill(bg);
    ellipseMode(RADIUS);
    ellipse(cx, cy, r, r);
    
    fill(plusColor);
    rect(cx-plusHeight*r, cy-plusWidth*r, 2*plusHeight*r, 2*plusWidth*r);
    rect(cx-plusWidth*r, cy-plusHeight*r, 2*plusWidth*r, 2*plusHeight*r);
  }
}

class Pointer extends SnapPoint {
  float triangleWidth = 0.5;
  
  Pointer(Box parent) {
    super(parent);
    setBackground(parent.getBackground());
    setOffsetX(parent.getWidth());
    setHeight(parent.getHeight());
    setWidth(h*triangleWidth);
  }
  
  void show() {
    fill(bg);
    triangle(x,y,x+triangleWidth*h,y+h/2,x,y+h);
    if (cheekyDevMode) if (mouseOver()) devSnap(this);
  }
  
  boolean mouseOver() {
    return mouseX > x && abs(mouseX-x)/triangleWidth+2*abs(mouseY-y-h/2) < h;
  }
}