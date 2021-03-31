class ScriptManager extends Selectable {
  ArrayList<Script> scripts = new ArrayList<Script>();
  
  Script selectedScript;

  void show() {
    for (Script script : scripts) if (!script.isLocked()) script.show();
  }

  Script addNewScript(ArrayList<Block> blocks) {
    Script newScript = new Script(blocks);
    scripts.add(newScript);
    return newScript;
  }

  void mousePress() {
    if (dragging == null) {
      for (int i = scripts.size() - 1; i >= 0; i--) {
        Script current = scripts.get(i);
        if (current.makeNewTag() == null) {
          if (current.mouseOver()) {
            if (mouseButton == RIGHT) {
              Script duplicate = current.duplicate();
              duplicate.drag();
            } else {
              if (current.getMouseOver() > 0) current.splitScript();
              current.drag();
            }
            break;
          }
        } else break;
      }
    }
  }

  void release(Script current) {
    scripts.remove(current);
    if (!drawer.toBin()) {
      Script above = checkAbove(current);
      Script below = checkBelow(current);
      if (above != null) above.addAtBottom(current);
      else if (below != null) below.addAtTop(current);
      else scripts.add(current);
    }
  }
  
  void scroll(float s) {
    for (Script script : scripts) {
      if (script.mouseOver()) {
        script.scroll(s);
        break;
      }
    }
  }
  
  void select(Script current) {
    selectedScript = current;
    current.select();
    super.select();
  }
  
  void deSelect() {
    selectedScript.deSelect();
    super.deSelect();
  }
  
  void type() {
    selectedScript.updateWidth();
  }

  Script checkAbove(Script current) {
    for (Script other : scripts) if (current != other) if (current.getTop().isNear(other.getBottom())) return other;
    return null;
  }

  Script checkBelow(Script current) {
    for (Script other : scripts) if (current != other) if (current.getBottom().isNear(other.getTop())) return other;
    return null;
  }
  
  Block checkForTag(Tag current) {
    for (Script other : scripts) {
      Block found = other.checkForTag(current);
      if (found != null) return found;
    }
    return null;
  }
  
  OperandBlock checkAttach(Tag current) {
    for (Script other : scripts) {
      OperandBlock found = other.checkAttach(current);
      if (found != null) return found;
    }
    return null;
  }
  
  void run() { for(Script script : scripts) script.run(); }
  
  void assemble() {
    ArrayList<Tag> blockTags = new ArrayList<Tag>();
    Script start = getStartingScript();
    if (start != null) start.getTags(0);
    for (Script script : scripts) if (script != start) blockTags.addAll(script.getTags(ram.nextRegister()));
    if (start != null) start.assemble(blockTags);
    for (Script script : scripts) if (script != start) script.assemble(blockTags);
  }
  
  Script getStartingScript() {
    for (Script script : scripts) if (script.isStartingScript()) return script;
    return null;
  }
  
  ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Scripts");
    int n = scripts.size();
    states.add(str(n));
    for (int k = 0; k < n; k++) states.addAll(scripts.get(k).saveState(k));
    return states;
  }
  
  void loadStates(ArrayList<String> states) {
    scripts = new ArrayList<Script>();
    int k = 0;
    while (states.contains("#Script " + k)) {
      int startIndex = states.indexOf("#Script " + k);
      int endIndex = states.contains("#Script " + (k+1)) ? states.indexOf("#Script " + (k+1)) : states.size();
      ArrayList<String> state = new ArrayList<String>(states.subList(startIndex + 1, endIndex));
      Script script = addNewScript(loadNewScript(state, k));
      script.loadState(state);
      k++;
    }
  }
  
  ArrayList<Block> loadNewScript(ArrayList<String> states, int k) {
    ArrayList<Block> blocks = new ArrayList<Block>();
    int dk = 0;
    while (states.contains("#Block " + dk)) {
      int startIndex = states.indexOf("#Block " + dk);    
      ArrayList<String> state = new ArrayList<String>(states.subList(startIndex + 1, states.size()));
      Block block = drawer.loadNewBlock(state, dk);
      if (state.contains("#Operand " + dk)) {
        int index = state.indexOf("#Operand " + dk);
        OperandBlock opBlock = (OperandBlock)block;
        opBlock.setOperandText(state.get(index + 1));
        opBlock.updateWidth();
        blocks.add(opBlock);
        if (state.contains("#OperandTag " + dk)) {
          int opIndex = state.indexOf("#OperandTag " + dk);
          if (state.contains("#OperandStartTag " + dk)) opBlock.addOpTag(tags.addTag(new StartTag(block)));
          else {
            Tag newTag = new Tag(opBlock);
            newTag.setText(state.get(opIndex + 1));
            newTag.updateWidth();
            opBlock.addOpTag(tags.addTag(newTag));
          }
        }
      } else blocks.add(block);
      if (state.contains("#Tag " + dk)) {
        int index = state.indexOf("#Tag " + dk);
        if (state.contains("#StartTag " + dk)) block.addTag(tags.addTag(new StartTag(block)));
        else {
          Tag newTag = new Tag(block);
          newTag.setText(state.get(index + 1));
          newTag.updateWidth();
          block.addTag(tags.addTag(newTag));
        }
      }
      dk++;
    }
    return blocks;
  }
}

class TagManager extends Selectable {
  ArrayList<Tag> tags = new ArrayList<Tag>();
  
  Tag selectedTag;

  void show() {
    for (Tag tag : tags) if (!tag.isLocked() && !tag.hasParent()) tag.show();
  }

  Tag addTag(Tag t) {
    tags.add(t);
    return t;
  }

  void mousePress() {
    if (dragging == null) {
      for (int i = tags.size() - 1; i >= 0; i--) {
        Tag current = tags.get(i);
        if (current.mouseOver()) {
          if (mouseButton == RIGHT) {
            Tag copy = current.copy();
            if (copy != null) addTag(copy).drag();
          } else {
            current.detach();
            current.drag();
          }
          break;
        }
      }
    }
  }

  void release(Tag current) {
    if (!drawer.toBin()) {
      Block tagBlock = scripts.checkForTag(current);
      OperandBlock attachBlock = scripts.checkAttach(current);
      if (tagBlock != null) tagBlock.addTag(current);
      else if (attachBlock != null) attachBlock.addOpTag(current);
    } else tags.remove(current);
  }
  
  void select(Tag current) {
    selectedTag = current;
    current.select();
    super.select();
  }
  
  void deSelect() {
    selectedTag.deSelect();
    super.deSelect();
  }
  
  void type() {
    selectedTag.updateWidth();
  }
  
  ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Tags");
    for (Tag tag : tags) states.addAll(tag.saveState());
    return states;
  }
  
  void loadStates(ArrayList<String> states) {
    tags = new ArrayList<Tag>();
    int k = 0;
    while (states.contains("#EmptyTag " + k)) {
      //int startIndex = states.indexOf("#EmptyTag " + k);
      //ArrayList<String> state = new ArrayList<String>(states.subList(startIndex + 1, states.size()));
      //Tag tag = new Tag(null);
      //tag.loadState(state);
      //k++;
    }
  }
}

class HardwareManager {
  ArrayList<Hardware> hardware = new ArrayList<Hardware>();
  
  HardwareManager() {
    hardware.add(ram = new RAM());
    hardware.add(cpu = new CPU());
    hardware.add(out = new Output());
    hardware.add(inp = new Input());
    hardware.add(alu = new ALU());
    hardware.add(acc = new Accumulator());
    hardware.add(pc  = new ProgramCounter());
  }
  
  void run() { for (Hardware component : hardware) component.run(); }
  
  void show() { for (Hardware component : hardware) component.show(); }
  
  void mousePress() {
    for (int i = hardware.size()-1; i >= 0; i--) hardware.get(i).mousePress();
  }
  
  void reset() {
    particles.clearAll(); // What do I want to reset???
    for (Hardware component : hardware) component.reset();
  }
  
  ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#Hardware");
    for (Hardware component : hardware) states.addAll(component.saveState());
    return states;
  }
  
  void loadStates(ArrayList<String> states) {
    for (Hardware component : hardware) component.loadState(states);
  }
  
  void helpOverlay(int helpMode) {
    if (helpMode == 3) {
      ram.helpOverlay();
      acc.helpOverlay();
    } else if (helpMode == 4) {
      cpu.helpOverlay();
    } else if (helpMode == 5) {
      pc.helpOverlay();
      alu.helpOverlay();
    } else if (helpMode == 6) {
      inp.helpOverlay();
      out.helpOverlay();
    }
  }
}

class ParticleManager {
  ArrayList<ParticleAttractor> attractors = new ArrayList<ParticleAttractor>();
  void clearAll() { for (ParticleAttractor p : attractors) p.clearAll(); }
  
  float particleSpeed = 0.5;
  float getParticleSpeed() { return particleSpeed; }
  void setParticleSpeed(float s) { particleSpeed = s; }
  
  void run() {
    setParticleSpeed(control.getParticleSpeed());
    for (ParticleAttractor p : attractors) p.run();
  }
  
  void show() { for (ParticleAttractor p : attractors) p.show(); }
  
  void addAttractor(ParticleAttractor p) { attractors.add(p); }
  
  ArrayList<String> saveStates() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#ParticleAttractors");
    //for (ParticleAttractor p : attractors) states.addAll(p.saveStates());
    return states;
  }
}

class ParticleAttractor extends Selectable {
  ArrayList<Particle> attractedParticles = new ArrayList<Particle>();
  
  void clearAll() { attractedParticles.clear(); }
  
  int input;
  Slate target;
  
  ParticleAttractor(Slate _target) {
    particles.addAttractor(this);
    target = _target;
  }
  
  Slate getTarget() { return target; }
  
  void addParticle(Particle particle) {
    particle.setTarget(this);
    attractedParticles.add(particle);
  }
  
  void run() { for (Particle p : attractedParticles) p.update(); }
  
  void show() { for (Particle p : attractedParticles) p.show(); }
  
  PVector targetGravity() {
    return new PVector(target.getCenterX(), target.getCenterY());
  }
  
  float distance(Particle p) { return dist(p.pos.x, p.pos.y, target.getCenterX(), target.getCenterY()); }
  
  Particle getInput() {
    for (Particle particle : attractedParticles) if (particle.arrived()) return particle;
    return null;
  }
  
  boolean gotInput() {
    for (Particle particle : attractedParticles) if (particle.arrived()) {
      input = particle.getIntData();
      removeParticle(particle);
      return true;
    }
    return false;
  }
  
  int input() { return input; }
 
  void removeParticle(Particle particle) { attractedParticles.remove(particle); }
}
