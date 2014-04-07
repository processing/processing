void initCubeMap() {
  sphereDetail(50);
  domeSphere = createShape(SPHERE, height/2.0f);
  domeSphere.rotateX(HALF_PI);
  domeSphere.setStroke(false);

  PGL pgl = beginPGL();

  envMapTextureID = IntBuffer.allocate(1);
  pgl.genTextures(1, envMapTextureID);
  pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, envMapTextureID.get(0));
  pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_S, PGL.CLAMP_TO_EDGE);
  pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_T, PGL.CLAMP_TO_EDGE);
  pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);
  pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MIN_FILTER, PGL.NEAREST);
  pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAG_FILTER, PGL.NEAREST);
  for (int i = PGL.TEXTURE_CUBE_MAP_POSITIVE_X; i < PGL.TEXTURE_CUBE_MAP_POSITIVE_X + 6; i++) {
    pgl.texImage2D(i, 0, PGL.RGBA8, envMapSize, envMapSize, 0, PGL.RGBA, PGL.UNSIGNED_BYTE, null);
  }

  // Init fbo, rbo
  fbo = IntBuffer.allocate(1);
  rbo = IntBuffer.allocate(1);
  pgl.genFramebuffers(1, fbo);
  pgl.bindFramebuffer(PGL.FRAMEBUFFER, fbo.get(0));
  pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, PGL.TEXTURE_CUBE_MAP_POSITIVE_X, envMapTextureID.get(0), 0);

  pgl.genRenderbuffers(1, rbo);
  pgl.bindRenderbuffer(PGL.RENDERBUFFER, rbo.get(0));
  pgl.renderbufferStorage(PGL.RENDERBUFFER, PGL.DEPTH_COMPONENT24, envMapSize, envMapSize);

  // Attach depth buffer to FBO
  pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.DEPTH_ATTACHMENT, PGL.RENDERBUFFER, rbo.get(0));    

  endPGL();

  // Load cubemap shader.
  cubemapShader = loadShader("cubemapfrag.glsl", "cubemapvert.glsl");
  cubemapShader.set("cubemap", 1);
}

void drawCubeMap() {
  PGL pgl = beginPGL();
  pgl.activeTexture(PGL.TEXTURE1);
  pgl.enable(PGL.TEXTURE_CUBE_MAP);  
  pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, envMapTextureID.get(0));     
  regenerateEnvMap(pgl);
  endPGL();
  
  drawDomeMaster();
  
  pgl.disable(PGL.TEXTURE_CUBE_MAP);
  pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, 0);    
}

void drawDomeMaster() {
  camera();
  ortho(0, width, 0, height);
  resetMatrix();
  shader(cubemapShader);
  shape(domeSphere);
  resetShader();
}

// Called to regenerate the envmap
void regenerateEnvMap(PGL pgl) {    
  // bind fbo
  pgl.bindFramebuffer(PGL.FRAMEBUFFER, fbo.get(0));

  // generate 6 views from origin(0, 0, 0)
  pgl.viewport(0, 0, envMapSize, envMapSize);    
  perspective(90.0f * DEG_TO_RAD, 1.0f, 1.0f, 1025.0f);  
  for (int face = PGL.TEXTURE_CUBE_MAP_POSITIVE_X; face < 
                  PGL.TEXTURE_CUBE_MAP_NEGATIVE_Z; face++) {
    resetMatrix();

    if (face == PGL.TEXTURE_CUBE_MAP_POSITIVE_X) {
      camera(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
    } else if (face == PGL.TEXTURE_CUBE_MAP_NEGATIVE_X) {
      camera(0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
    } else if (face == PGL.TEXTURE_CUBE_MAP_POSITIVE_Y) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, -1.0f);  
    } else if (face == PGL.TEXTURE_CUBE_MAP_NEGATIVE_Y) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
    } else if (face == PGL.TEXTURE_CUBE_MAP_POSITIVE_Z) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, -1.0f, 0.0f);    
    }
    
    scale(-1, 1, -1);
    translate(-width * 0.5f, -height * 0.5f, -500);

    pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, face, envMapTextureID.get(0), 0);

    drawScene(); // Draw objects in the scene
    flush(); // Make sure that the geometry in the scene is pushed to the GPU    
    noLights();  // Disabling lights to avoid adding many times
    pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, face, 0, 0);
  }
}
