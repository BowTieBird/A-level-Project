class Selectable {
  boolean thisSelected = false;
  boolean isSelected() { return thisSelected; }
  
  void select() {
    selected = this;
    thisSelected = true;
  }
  
  void deSelect() {
    selected = null;
    thisSelected = false;
  }
}

class Colored extends Selectable {
  color bg = color(255);
  color getBackground() { return bg; }
  void setBackground(color _bg) { bg = _bg; }
}

class Slate extends Colored {
    /*
  (_x, _y) __dx__
          |      |
         dy      dy
          |__dx__(x, y)____w_____
                      |          |
                      |          |
                      h (cx, cy) h
                      |          |
                      |____w_____|
  */
  
  float  w = 10, h = 10;
  float  x = 0,  y = 0;
  float cx = 5, cy = 5;
  float dx = 0, dy = 0;
  
  float getWidth() { return w; }
  float getHeight() { return h; }
  float getX() { return x; }
  float getY() { return y; }
  float getCenterX() { return cx; }
  float getCenterY() { return cy; }
  
  void setOffsetX(float _dx) { dx = _dx; }
  void setOffsetY(float _dy) { dy = _dy; }
  
  void setWidth(float _w) {
     w = _w;
    cx = x + w/2;
  }
  
  void setHeight(float _h) {
     h = _h;
    cy = y + h/2;
  }
  
  void setX(float _x) { 
     x = _x + dx;
    cx = x + w/2;
  }
 
  void setY(float _y) { 
     y = _y + dy;
    cy = y + h/2;
  }
  
  void setCenterX(float _cx) {
    cx = _cx + dx;
     x = cx - w/2;
  }
  
  void setCenterY(float _cy) {
    cy = _cy + dy;
     y = cy - h/2;
  }

  void setOffset(float dx, float dy) {
    setOffsetX(dx);
    setOffsetY(dy);
  }

  void setSize(float w, float h) {
    setWidth(w);
    setHeight(h);
  } 
  
  void setPos(float x, float y) {
    setX(x);
    setY(y);
  }
  
  void setCenter(float cx, float cy) {
    setCenterX(cx);  
    setCenterY(cy);
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add(str(x));
    state.add(str(y));
    state.add(str(w));
    state.add(str(h));
    state.add(str(dx));
    state.add(str(dy));
    return state;
  }
  
  void loadState(ArrayList<String> state) {
    setOffset(int(state.get(4)), int(state.get(5)));
    setSize(int(state.get(2)), int(state.get(3)));
    setPos(constrain(int(state.get(0)), EDGE_BOUNDARY, W - EDGE_BOUNDARY), constrain(int(state.get(1)), EDGE_BOUNDARY, H - EDGE_BOUNDARY));
  }
}

class Child extends Slate {
  Box parent;  
  void setParent(Box _parent) { parent = _parent;  }  
  boolean hasParent() { return parent != null; }
  void updatePos() { setPos(parent.getX(), parent.getY()); }
}

class Box extends Child {  
  void show() {
    fill(bg);
    rect(x, y, w, h);
    
    if(cheekyDevMode) if (mouseOver()) devDraw(this);
  }  
  
  boolean mouseOver() {
    return mouseX/SIZE_MULT > x && mouseX/SIZE_MULT < x+w && mouseY/SIZE_MULT > y && mouseY/SIZE_MULT < y+h;
  }
}

class LBox extends Box {
  color labelC = color(255, 180);
  float labelSize = 11;
  float labelOffset = 0;
  String label = "";
  
  int leftAlign = CENTER;
  int topAlign = BOTTOM;
  void setLabelLeftAlign(int l) { leftAlign = l; }
  void setLabelTopAlign(int t) { topAlign = t; }
  
  void setLabel(String l) { label = l; }
  void setLabelColor(color c) { labelC = c; }
  void setLabelSize(float s) { labelSize = s; }
  void setLabelOffset(float o) { labelOffset = o; }
  
  void show() {
    super.show();
    showLabel();
  }
  
  void showLabel() {
    fill(labelC);
    textSize(labelSize);
    textAlign(CENTER, CENTER);
         if (leftAlign == LEFT)  text(label, x-labelOffset, cy);
    else if (topAlign == BOTTOM) text(label, cx, y+h-labelOffset);
    else if (topAlign == CENTER) text(label, cx, cy-labelOffset);
    else if (topAlign == TOP)    text(label, cx, y+labelOffset);
  }
}

class Draggable extends LBox {
  boolean locked = false;
  boolean isLocked() { return locked; }
  
  void lock() {
    locked = true;
    dragging = this;
    setOffset(x - mouseX/SIZE_MULT, y - mouseY/SIZE_MULT);
  }
  
  void release() {
    locked = false;
    dragging = null;
    setOffset(0, 0);
  }
  
  void drag() {
    if (!locked) lock();
    setPos(constrain(mouseX/SIZE_MULT, EDGE_BOUNDARY, W - EDGE_BOUNDARY), constrain(mouseY/SIZE_MULT, EDGE_BOUNDARY, H - EDGE_BOUNDARY));
  }
  
  void mousePress() {
    if (dragging == null) if (mouseOver()) drag();
  }
}

class TextBox extends Draggable {
  color fg = color(0);
  void setTextColor(color _fg) { fg = _fg; }
  
  int leftAlign = CENTER;
  int topAlign = CENTER;
  void setLeftAlign(int l) { leftAlign = l; }
  void setTopAlign(int t) { topAlign = t; }
  
  String contents = "";
  float contentsPad = 2;
  float textOffset = 0;
  int min = 4;
  int max = 50;
  
  String getText() { return contents; }
  int getIntText() { return int(contents); }
  int getLength() { return contents.length(); }
  
  void setText(String _contents) { contents = _contents; }
  void setText(int _contents) {
    contents = nf(_contents % int(pow(10, min)), min);
  }
  
  void setPadding(float padding) { contentsPad = padding; }
  void setTextSize() { textSize(h - 2*contentsPad); }
  void setTextOffset(float _offset) { textOffset = _offset; }
  
  void setMin(int _min) { min = _min; }
  void setMax(int _max) { max = _max; }
  
  void setLimits(int min, int max) {
    setMin(min);
    setMax(max);
  }
  
  void show() {
    super.show();
    showText();
  }
  
  void showText() {
    textAlign(leftAlign, topAlign);
    setTextSize();
    fill(fg);
    if (leftAlign == LEFT) text(contents, x + textOffset, cy - contentsPad);
    if (leftAlign == CENTER) text(contents, cx + textOffset, cy - contentsPad);
  }
  
  void updateLength() {
    int l = getLength();
    float c = charWidth();
    if (l < min) setWidth(floor(min*c + 2*contentsPad));
    else setWidth(floor(textWidth(contents))+2*contentsPad);
  }
  
  float charWidth() {
    setTextSize();
    return textWidth('a');
  }
}

class Button extends TextBox {
  color hbg, ubg;
  color fg, hfg, ufg;
  boolean highlighted = false;
    
  void highlight() {
    highlighted = true;
    bg = hbg;
    fg = hfg;
  }
  
  void unHighlight() {
    highlighted = false;
    bg = ubg;
    fg = ufg;
  }
  
  void setColors(color _bg, color _hbg, color _fg, color _hfg) {
    ubg = bg = _bg;
    hbg = _hbg;
    ufg = fg = _fg;
    hfg = _hfg;
  }
  
  void show() {
    if (highlightCondition()) highlight();
    else if (highlighted) unHighlight();
    super.show();
  }
  
  boolean highlightCondition() {
    return mouseOver();
  }
}

class SliderBase extends Box {
  boolean vertical = true;
  SliderBtn slider = new SpeedBtn();

  float min = 0.05;
  float max = 100;
  float scl = 10000;
  float inc = 10;
  float lrp = 2.75;
  float start = 1.0;
  
  float value = 1.0;
  float getValue() { return value; }
  float top;
  float getTop() { return top; }
  
  void setUpSlider() {
    slider.setParent(this);
    slider.updatePos();
    slider.setSize(getWidth(), getWidth());
    top = y + h - slider.getHeight();
    slider.setY(calculateY(start));
  }
  
  void setColors(color bg, color sbg, color shbg, color sfg, color shfg) {
    setBackground(bg);
    slider.setColors(sbg, shbg, sfg, shfg);
  }
  
  void show() {
    super.show();
    slider.show();
  }
  
  void mousePress() {
    select();
    if (slider.mouseOver()) slide();
  }
  
  void slide() {
    slider.drag();
  }
  
  float calculateValue() {
    return calculateValue(slider.getY() - y);
  }
  
  float calculateValue(float yval) {
    return value = pow(yval, lrp) / scl + min;
  }
  
  float calculateY(float v) {
    value = v;
    return pow(scl*(v - min), 1/lrp) + y;
  }
  
  boolean atMax() {
    return slider.getY() == y + h - slider.getHeight();
  }
}

class SliderBtn extends Button {
  void drag() {
    super.drag();
    setOffsetX(0);
    SliderBase p = (SliderBase)parent;
    setPos(parent.getX(), constrain(mouseY/SIZE_MULT, p.getY() - dy, p.getTop() - dy));
    p.calculateValue();
  }
  
  void release() {
    super.release();
    SliderBase p = (SliderBase)parent;
    p.deSelect();
  }
  
  boolean highlightCondition() {
    SliderBase p = (SliderBase)parent;
    return y == p.getTop();
  }
}

class Typable extends TextBox {
  float flashTime = 15;
  float flashCount = 0;
  boolean cursorFlashing = false;
  float cursorWidth = 10;
  
  Typable() {
    setLeftAlign(LEFT);
  }
  
  void show() {
    super.show();
    if (thisSelected) flashCursor();
  }
  
  void select() {
    super.select();
    typing = this;
    resetFlash();
  }
  
  void deSelect() {
    super.deSelect();
    typing = null;
  }
  
  void flashCursor() {
    if (flashCount <= 0) {
      cursorFlashing ^= true;
      flashCount = flashTime;
    }
    flashCount--;
    if (cursorFlashing) showCursor();
  }
  
  void resetFlash() {
    cursorFlashing = true;
    flashCount = flashTime;
  }
  
  void showCursor() {
    int l = getLength();
    float c = charWidth();
    fill(fg);
    rect(x + l*c + contentsPad, y + contentsPad, cursorWidth, h - 2*contentsPad);
  }
    
  void type(char c) {
    resetFlash();
    int l = getLength();
    if (c == BACKSPACE) {
      if (l > 0) contents = contents.substring(0, l-1);
    } else if (l < max) contents += c;
    updateLength();
  }
}

class PopUp extends LBox {
  float popUpWidth = 200, popUpHeight = 100;
  float triangleWidth = 20, triangleHeight = 20;
  float getTotalWidth() { return getWidth() + triangleWidth; }
  
  float textboxX, textboxY = 20;
  float textboxWidth = 180, textboxHeight = 35;
  float labelSize = 25;
  int inputMin = 8, inputMax = 32;
  String getText() { return textbox.getText(); }  
  
  Typable textbox = new Typable();
  
  PopUp(Box parent, String label, String t) {
    setParent(parent);
    setSize(popUpWidth, popUpHeight);
    setUpInput();
    setUpLabel(label);    
    textbox.setText(t);
    updateWidth();
  }  
   
  void setUpInput() {
    textbox.setParent(this);
    textbox.setLimits(inputMin, inputMax);
    textbox.setSize(textboxWidth, textboxHeight);
    textbox.updateLength();
    textboxX = popUpWidth/2 - textbox.getWidth()/2;
    textbox.setOffset(textboxX, textboxY);
  }
  
  void setUpLabel(String label) {
    setLabel(label);
    setLabelOffset(labelSize);
    setLabelSize(labelSize);
  }
  
  void show() {
    super.show();
    textbox.show();
    pointer();
  }
  
  void select() {
    textbox.select();
    super.select();    
  }
  
  void deSelect() {
    textbox.deSelect();
    super.deSelect();
    control.clearPopUps(); // :P
  }
  
  void updatePos() {
    setOffsetX(-getTotalWidth());
    setX(parent.getX());
    setCenterY(parent.getCenterY());
    textbox.updatePos();
  }
    
  void updateWidth() {
    textbox.updateLength();
    setWidth(2*textboxX + textbox.getWidth());
    updatePos();
  }
  
  void pointer() {
    fill(bg);
    triangle(x+w,cy-triangleHeight,x+w+triangleWidth,cy,x+w,cy+triangleHeight);
  }
}

class FTextBox extends TextBox {
  FTextBox(int n) {
    setMin(n);
    setText(0);
  }
}

class Hardware extends Draggable {
  void run() {}
  void reset() {}
}
