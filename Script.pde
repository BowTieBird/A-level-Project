class Script extends Draggable {
  ArrayList<Block> script = new ArrayList<Block>();
  Block selectedBlock;

  Slot top;
  Tab bottom;

  float getTotalHeight() {
    float h = 0;
    for (Block block : script) h += block.getTotalHeight();
    return h;
  }
  
  ArrayList<Block> getScript() { return script; }
  
  int getSize() { return script.size(); }
  
  Slot getTop() { return top; }
  Tab getBottom() { return bottom; }

  Script(ArrayList<Block> blocks) {
    script.addAll(blocks);
    for (Block block : blocks) block.setParent(this);
    updateConnector();
    Block first = script.get(0);
    setPos(first.getX(), first.getY());
  }
  
  void updateConnector() {
    top = script.get(0).getSlot();
    bottom = script.get(script.size() - 1).getTab();
  }
  
  void setPos(float _x, float _y) {
    super.setPos(_x, _y);
    update();
  }
  
  void update() {
    float h = 0;
    for (Block block : script) {
      block.setPos(x, y + h);
      h += block.getTotalHeight();
    }
  }

  void show() {
    for (Block block : script) { block.show(); }
  }
  
  boolean mouseOver() {
    for (Block block : script) if (block.mouseOver()) return true;
    return false;
  }

  int getMouseOver() {
    for (Block block : script) if (block.mouseOver()) return script.indexOf(block);
    return -1;
  }
  
  void release() {
    scripts.select(this);
    super.release();
    scripts.release(this);
  }
  
  void select() {
    int index = getMouseOver();
    if (index == -1) index = 0;
    selectedBlock = script.get(index);
    selectedBlock.select();
    super.select();
  }
  
  void deSelect() {
    selectedBlock.deSelect();
    super.deSelect();
  }
  
  void updateWidth() {
    selectedBlock.updateWidth();
  }
    
  void addAtTop(Script other) {
    float newY = y - other.getTotalHeight();
    script.addAll(0, other.getScript());
    updateConnector();
    setPos(x, newY);
  }
  
  void addAtBottom(Script other) {
    int n = script.size();
    script.addAll(n, other.getScript());
    updateConnector();
    update();
  }
  
  void splitScript() {
    int index = getMouseOver();
    Script split = scripts.addNewScript(new ArrayList(script.subList(0, index)));
    float newY = y + split.getTotalHeight();
    for (int i = 0; i < index; i++) script.remove(0);
    updateConnector();
    setPos(x, newY);
  }
  
  Script duplicate() {
    int index = getMouseOver();
    ArrayList<Block> newScript = new ArrayList<Block>();
    for (int i = index; i < script.size(); i++) newScript.add(script.get(i).copy());
    return scripts.addNewScript(newScript);
  }
  
  Block checkForTag(Tag current) {  
    for (Block other : script) {
      Block found = other.checkForTag(current);
      if (found != null) return found;
    }
    return null;
  }
  
  OperandBlock checkAttach(Tag current) {  
    for (Block other : script) {
      if (other instanceof OperandBlock) {
        OperandBlock found = (OperandBlock)other;
        found = found.checkAttach(current);
        if (found != null) return found;
      }
    }
    return null;
  }
  
  Tag makeNewTag() {
    for (Block block : script) {
      Tag found = block.makeNewTag();
      if (found != null) return found;
    }
    return null;
  }
  
  void scroll(float s) {
    for (Block block : script) {
      block.change(s);
      break;
    }
  }
  
  void assemble(ArrayList<Tag> blockTags) {
    for (Block block : script) block.assembleTag(blockTags);
  }
  
  void run() {
    for (Block block : script) { block.run(); }
  }
  
  boolean isStartingScript() {
    for (Block block : script) if (block.hasStartTag()) return true;
    return false;    
  }  
  
  ArrayList<Tag> getTags(int k) {
    ArrayList<Tag> getTags = new ArrayList<Tag>();
    for (int dk = 0; dk < script.size(); dk++) {
      Block block = script.get(dk);
      block.setRegIndex(k + dk);
      ram.getRegister(k + dk).setWillHaveData(true);
      if (!(block.getTag() == null || block.getTag() instanceof StartTag)) getTags.add(block.getTag());
    }
    return getTags;
  }
  
  ArrayList<String> saveState(int k) {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Script " + k);
    states.addAll(super.saveState());
    int n = script.size();
    states.add(str(n));
    for (int dk = 0; dk < n; dk++) states.addAll(script.get(dk).saveState(dk));
    return states;
  }
}