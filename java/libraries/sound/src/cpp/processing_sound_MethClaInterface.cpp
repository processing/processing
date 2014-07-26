#include <stdio.h>
#include <stdlib.h>
#include <cassert>
#include <array>
#include <vector>
#include <typeinfo>
#include <mutex> 
#include <atomic>

#include "processing_sound_MethClaInterface.h"
#include "methcla/file.hpp"
#include "methcla/engine.hpp"
#include "methcla/plugins/sine.h"
#include "methcla/plugins/saw.h"
#include "methcla/plugins/tri.h"
#include "methcla/plugins/pulse.h"
#include "methcla/plugins/patch-cable.h"
#include "methcla/plugins/soundfile_api_libsndfile.h"
#include "methcla/plugins/sampler.h"
#include "methcla/plugins/whitenoise.h"
#include "methcla/plugins/pinknoise.h"
#include "methcla/plugins/brownnoise.h"
#include "methcla/plugins/node-control.h"
#include "methcla/plugins/pan2.h"
#include "methcla/plugins/soundfile_api_mpg123.h"
#include "methcla/plugins/ampfol.h"
#include "methcla/plugins/fft.h"
#include "methcla/plugins/hpf.h"
#include "methcla/plugins/lpf.h"
#include "methcla/plugins/bpf.h"
#include "methcla/plugins/delay.h"
#include "methcla/plugins/reverb.h"
#include "methcla/plugins/audio_in.h"

#define OUTPUT_BUFFER_SIZE 1024
#define SNDF_BUFFER_LEN 1024

#define MAX_CHANNELS    4

Methcla::Engine* m_engine;
Methcla::Engine& engine() { return *m_engine; }
std::mutex mutex_fft_in;
std::mutex mutex_fft_out;
std::mutex mutex_amp_in;
std::mutex mutex_amp_out;


static Methcla_Time kLatency = 0.1;

struct ServerValue{
    ServerValue() :
    amp(0),
    id(-1)
    {}
    float amp;
    int id;
};

struct ServerArray{
    ServerArray() :
    fftSize(512),
    fft(fftSize),
    id(-1)
    {}
    int fftSize;
    std::vector<float> fft;
    int id;
};

// Engine

JNIEXPORT jint JNICALL Java_processing_sound_MethClaInterface_engineNew (JNIEnv *, jobject, jint sampleRate, jint bufferSize){
    
    Methcla::EngineOptions options;
    options.audioDriver.bufferSize = bufferSize;
    options.audioDriver.numInputs = 2;
    options.realtimeMemorySize = 1024 * 1024 * 20;
    options.maxNumNodes = 1024 * 20;
    options.addLibrary(methcla_plugins_sine)
           .addLibrary(methcla_plugins_saw)
           .addLibrary(methcla_plugins_tri)
           .addLibrary(methcla_plugins_pulse)
           .addLibrary(methcla_soundfile_api_libsndfile)
           .addLibrary(methcla_plugins_patch_cable)
           .addLibrary(methcla_plugins_sampler)
           .addLibrary(methcla_plugins_white_noise)
           .addLibrary(methcla_plugins_pink_noise)
           .addLibrary(methcla_plugins_brown_noise)
           .addLibrary(methcla_plugins_node_control)
           .addLibrary(methcla_plugins_pan2)
           .addLibrary(methcla_plugins_amplitude_follower)
           .addLibrary(methcla_plugins_hpf)
           .addLibrary(methcla_plugins_lpf)
           .addLibrary(methcla_plugins_bpf)                      
           .addLibrary(methcla_plugins_delay)
           .addLibrary(methcla_plugins_reverb)
           .addLibrary(methcla_plugins_fft)
           .addLibrary(methcla_plugins_audioin);

    m_engine = new Methcla::Engine(options);

    return 1;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_engineStart(JNIEnv *env, jobject object){
    engine().start();
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_engineStop(JNIEnv *env, jobject object){
        
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.freeAll(engine().root());
    request.closeBundle();
    request.send();

    engine().stop();
    //delete m_engine;

};

// General Synth

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_synthStop(JNIEnv *env, jobject object, jintArray nodeId){
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.free(m_nodeId[0]);
    request.free(m_nodeId[1]);

    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

};

// General Oscillator set method

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_oscSet (JNIEnv *env, jobject object, jfloat freq, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){

    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0 , freq);
    request.set(m_nodeId[0], 1 , amp);
    request.set(m_nodeId[0], 2 , add);    
    request.set(m_nodeId[1], 0, pos);
    request.closeBundle();
    request.send(); 

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_oscAudioSet(JNIEnv *env, jobject object, jintArray freq, jintArray amp, jintArray add, jintArray pos, jintArray nodeId){

    jint* m_freq = env->GetIntArrayElements(freq, 0); 
    jint* m_amp = env->GetIntArrayElements(amp, 0); 
    jint* m_add = env->GetIntArrayElements(add, 0); 
    jint* m_pos = env->GetIntArrayElements(pos, 0); 
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    if (m_freq[0] != -1)
    {
        Methcla::AudioBusId freq_bus = m_engine->audioBusId().alloc();
        //request.set(m_nodeId[0], 0 , 0);
        //request.free(m_freq[1]);
        request.mapOutput(m_freq[0], 0, freq_bus);
        request.mapInput(m_nodeId[0], 0, freq_bus);

        std::cout << "freq" << std::endl;
    }
    
    if (m_amp[0] != -1)
    { 
        
        Methcla::AudioBusId amp_bus = m_engine->audioBusId().alloc();
        //request.set(m_nodeId[0], 1 , 0);
        //request.free(m_amp[1]);
        request.mapOutput(m_amp[0], 0, amp_bus);
        request.mapInput(m_nodeId[0], 1, amp_bus);

        std::cout << "amp" << std::endl;
    }

    if (m_add[0] != -1)
    {
        Methcla::AudioBusId add_bus = m_engine->audioBusId().alloc();
        request.set(m_nodeId[0], 2 , 0);
        request.free(m_add[1]);
        request.mapOutput(m_add[0], 0, add_bus);
        request.mapInput(m_nodeId[0], 2, add_bus);

        std::cout << "add" << std::endl;
    }

    if (m_pos[0] != -1)
    {
        Methcla::AudioBusId pos_bus = m_engine->audioBusId().alloc();
        request.set(m_nodeId[1], 0 , 0);
        request.free(m_pos[1]);
        request.mapOutput(m_pos[0], 0, pos_bus);
        request.mapInput(m_nodeId[1], 0, pos_bus);

        std::cout << "pos" << std::endl;
    }

    request.closeBundle();
    request.send(); 

    env->ReleaseIntArrayElements(freq, m_freq, 0);
    env->ReleaseIntArrayElements(amp, m_amp, 0);
    env->ReleaseIntArrayElements(add, m_add, 0);
    env->ReleaseIntArrayElements(pos, m_pos, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

// SineOsc

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_sinePlay(JNIEnv *env, jobject object, jfloat freq, jfloat amp, jfloat add, jfloat pos){

    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
        METHCLA_PLUGINS_SINE_URI, 
        engine().root(), 
        {freq, amp, add}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );
    
    engine().addNotificationHandler(engine().freeNodeIdHandler(synth.id()));
    engine().addNotificationHandler(engine().freeNodeIdHandler(pan.id()));
    
    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
        
    return nodeId;
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_sawPlay(JNIEnv *env, jobject object, jfloat freq, jfloat amp, jfloat add, jfloat pos){
    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
        METHCLA_PLUGINS_SAW_URI, 
        engine().root(), 
        {freq, amp, add}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );
    
    engine().addNotificationHandler(engine().freeNodeIdHandler(synth.id()));
    engine().addNotificationHandler(engine().freeNodeIdHandler(pan.id()));
    
    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
        
    return nodeId;
};


JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_triPlay(JNIEnv *env, jobject object, jfloat freq, jfloat amp, jfloat add, jfloat pos){

    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
        METHCLA_PLUGINS_TRI_URI, 
        engine().root(), 
        {freq, amp, add}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );
    
    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
        
    return nodeId;
}

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_sqrPlay(JNIEnv *env, jobject object, jfloat freq, jfloat amp, jfloat add, jfloat pos){
    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
        METHCLA_PLUGINS_PULSE_URI, 
        engine().root(), 
        {freq, 0.5, amp*2.f, add-1.f}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );

    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();
            
    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return nodeId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_sqrSet(JNIEnv *env, jobject object, jfloat freq, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){
    
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0 , freq);
    request.set(m_nodeId[0], 0 , 0.5f);
    request.set(m_nodeId[0], 2 , amp);
    request.set(m_nodeId[1], 0 , pos);
    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_pulsePlay(JNIEnv *env, jobject object, jfloat freq, jfloat width, jfloat amp, jfloat add, jfloat pos){
    
    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
        METHCLA_PLUGINS_PULSE_URI, 
        engine().root(), 
        {freq, width, amp, add}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );

    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();
            
    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return nodeId;
}

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_pulseSet(JNIEnv *env, jobject object, jfloat freq, jfloat width, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){
    
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0 , freq);
    request.set(m_nodeId[0], 1 , width);
    request.set(m_nodeId[0], 2 , amp);
    request.set(m_nodeId[1], 0 , pos);
    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_audioInPlay(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos, jint in){

    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
        METHCLA_PLUGINS_AUDIOIN_URI, 
        engine().root(), 
        {amp, add, pos}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );

    request.mapInput(synth.id(), 0, Methcla::AudioBusId(in), Methcla::kBusMappingExternal);
    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);
    

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();
            
    m_nodeId[0]=synth.id();
    m_nodeId[1]= pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return nodeId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_audioInSet(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0 , amp);
    request.set(m_nodeId[0], 1 , add);    
    request.set(m_nodeId[1], 0 , pos);
    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};



JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_soundFileInfo(JNIEnv *env, jobject object, jstring path){
    const char *str = env->GetStringUTFChars(path, 0);

    Methcla::SoundFile file(engine(), str);

    jintArray info = env->NewIntArray(3);
    jint *temp = env->GetIntArrayElements(info, NULL);

    temp[0] = file.info().frames;
    temp[1] = file.info().samplerate;
    temp[2] = file.info().channels;
    
    env->ReleaseIntArrayElements(info, temp, 0);
    env->ReleaseStringUTFChars(path, str);  

    return info;
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_soundFilePlayMono (JNIEnv *env, jobject object, jfloat rate, jfloat pos, jfloat amp, jfloat add, jboolean loop, jstring path, jfloat dur, jint cue){
 
    const char *str = env->GetStringUTFChars(path, 0);

    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::Request request(engine());
    Methcla::NodeTreeStatistics results;

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();

    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_SAMPLER_URI,
            engine().root(),
            { amp, rate },
            { Methcla::Value(str),
              Methcla::Value(loop),
              Methcla::Value(cue) }
    );
    
    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );
    
    auto after = request.synth(
            METHCLA_PLUGINS_DONE_AFTER_URI,
            engine().root(),
            { },
            { Methcla::Value(dur) }
    );

    engine().addNotificationHandler(engine().freeNodeIdHandler(synth.id()));
    engine().addNotificationHandler(engine().freeNodeIdHandler(pan.id()));
    engine().addNotificationHandler(engine().freeNodeIdHandler(after.id()));
                                    
    request.mapOutput(synth.id(), 0, bus);
    request.mapInput(pan.id(), 0, bus);
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.whenDone(after.id(), Methcla::kNodeDoneFreeSelf | Methcla::kNodeDoneFreePreceeding);
    request.activate(synth.id());
    request.activate(pan.id());
    if (loop == false)
    {
        request.activate(after.id());
    }   
    request.closeBundle();
  
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    //results = engine().getNodeTreeStatistics();
    //std::cout << results.numSynths << std::endl;

    env->ReleaseStringUTFChars(path, str);  
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return nodeId;
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_soundFilePlayMulti(JNIEnv *env, jobject object, jfloat rate, jfloat amp, jfloat add, jboolean loop, jstring path, jfloat dur, jint cue){
    const char *str = env->GetStringUTFChars(path, 0);

    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::Request request(engine());
    Methcla::NodeTreeStatistics results;

    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_SAMPLER_URI,
            engine().root(),
            { amp, rate },
            { Methcla::Value(str),
              Methcla::Value(loop),
              Methcla::Value(cue) }
    );
    
    auto after = request.synth(
            METHCLA_PLUGINS_DONE_AFTER_URI,
            engine().root(),
            { },
            { Methcla::Value(dur) }
    );

    request.mapOutput(synth.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(synth.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.whenDone(after.id(), Methcla::kNodeDoneFreeSelf | Methcla::kNodeDoneFreePreceeding);
    request.activate(synth.id());
    request.activate(after.id());
    request.closeBundle();
    
    engine().addNotificationHandler(engine().freeNodeIdHandler(synth.id()));
    engine().addNotificationHandler(engine().freeNodeIdHandler(after.id()));
  
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=after.id();

    //results = engine().getNodeTreeStatistics();
    //std::cout << results.numSynths << std::endl;

    env->ReleaseStringUTFChars(path, str);  
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return nodeId;
}

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_soundFileSetMono (JNIEnv *env, jobject object, jfloat rate, jfloat pos, jfloat amp, jfloat add, jintArray nodeId){

    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0, amp);
    request.set(m_nodeId[0], 1, rate);  
    request.set(m_nodeId[1], 0, pos);  
    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_soundFileSetStereo(JNIEnv *env, jobject object, jfloat rate, jfloat amp, jfloat add, jintArray nodeId){

    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0, amp);
    request.set(m_nodeId[0], 1, rate);  
    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0); 

};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_whiteNoisePlay(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos){
    
    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
        
    request.openBundle(Methcla::immediately);

    auto synth = request.synth(
            METHCLA_PLUGINS_WHITE_NOISE_URI, 
            engine().root(), 
            { amp, add },
            {Methcla::Value(0.0)}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );
    
    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
        
    return nodeId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_whiteNoiseSet(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){
    
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0, amp);
    request.set(m_nodeId[1], 0, pos);
    request.closeBundle();
    request.send(); 

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_pinkNoisePlay(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos){
    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);

    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
        
    request.openBundle(Methcla::immediately);

    auto synth = request.synth(
            METHCLA_PLUGINS_PINK_NOISE_URI, 
            engine().root(), 
            { amp, add },
            {}
    );

    auto pan = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            {pos, 1.f},
            {Methcla::Value(1.f)}
    );
    
    request.mapOutput(synth.id(), 0, bus); 
    request.mapInput(pan.id(), 0, bus); 
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);

    request.activate(synth.id());
    request.activate(pan.id());

    request.closeBundle();
    request.send();

    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
        
    return nodeId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_pinkNoiseSet(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){
    
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0, amp);
    request.set(m_nodeId[1], 0, pos);
    request.closeBundle();
    request.send(); 

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_brownNoisePlay(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos){
    jintArray nodeId = env->NewIntArray(2);
    jint *m_nodeId = env->GetIntArrayElements(nodeId, NULL);
    
    Methcla::AudioBusId bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
                               METHCLA_PLUGINS_BROWN_NOISE_URI,
                               engine().root(),
                               { amp, add },
                               {}
                               );
    
    auto pan = request.synth(
                             METHCLA_PLUGINS_PAN2_URI,
                             engine().root(),
                             {pos, 1.f},
                             {Methcla::Value(1.f)}
                             );
    
    request.mapOutput(synth.id(), 0, bus);
    request.mapInput(pan.id(), 0, bus);
    request.mapOutput(pan.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.mapOutput(pan.id(), 1, Methcla::AudioBusId(1), Methcla::kBusMappingExternal);
    
    request.activate(synth.id());
    request.activate(pan.id());
    
    request.closeBundle();
    request.send();
    
    m_nodeId[0]=synth.id();
    m_nodeId[1]=pan.id();
    
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
    
    return nodeId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_brownNoiseSet(JNIEnv *env, jobject object, jfloat amp, jfloat add, jfloat pos, jintArray nodeId){
    
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0);
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(m_nodeId[0], 0, amp);
    request.set(m_nodeId[1], 0, pos);
    request.closeBundle();
    request.send(); 
    
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
};


JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_envelopePlay(JNIEnv *env, jobject object, jintArray nodeId, jfloat attackTime, jfloat sustainTime, jfloat sustainLevel, jfloat releaseTime){
  
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    jintArray returnId = env->NewIntArray(2);
    jint *m_returnId = env->GetIntArrayElements(returnId, NULL);

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();


    const std::list<Methcla::Value> envOptions =
                { Methcla::Value(attackTime)
                , Methcla::Value(sustainTime)
                , Methcla::Value(sustainLevel)
                , Methcla::Value(releaseTime)
                };
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_ASR_ENVELOPE_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {},
            envOptions
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());

    request.closeBundle();
    request.send();

    m_returnId[0]=synth.id();
    m_returnId[1]=m_nodeId[1];

    env->ReleaseIntArrayElements(returnId, m_returnId, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return returnId;
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_highPassPlay(JNIEnv *env, jobject object, jintArray nodeId, jfloat freq){
  
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    jintArray returnId = env->NewIntArray(2);
    jint *m_returnId = env->GetIntArrayElements(returnId, NULL);

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_HPF_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {freq},
            {}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());

    request.closeBundle();
    request.send();
    
    m_returnId[0]=synth.id();
    m_returnId[1]=m_nodeId[1];

    env->ReleaseIntArrayElements(returnId, m_returnId, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
    
    return returnId;
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_lowPassPlay(JNIEnv *env, jobject object, jintArray nodeId, jfloat freq){
  
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    jintArray returnId = env->NewIntArray(2);
    jint *m_returnId = env->GetIntArrayElements(returnId, NULL);

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_LPF_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {freq},
            {}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());

    request.closeBundle();
    request.send();
    
    m_returnId[0]=synth.id();
    m_returnId[1]=m_nodeId[1];

    env->ReleaseIntArrayElements(returnId, m_returnId, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);
    
    return returnId;
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_bandPassPlay(JNIEnv *env, jobject object, jintArray nodeId, jfloat freq, jfloat bw){
  
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    jintArray returnId = env->NewIntArray(2);
    jint *m_returnId = env->GetIntArrayElements(returnId, NULL);

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_BPF_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {freq, bw},
            {}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());

    request.closeBundle();
    request.send();
    
    m_returnId[0]=synth.id();
    m_returnId[1]=m_nodeId[1];

    env->ReleaseIntArrayElements(returnId, m_returnId, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return returnId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_filterSet(JNIEnv *env, jobject object, jfloat freq, jint nodeId){

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(nodeId, 0, freq);
    request.closeBundle();
    request.send(); 
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_filterBwSet(JNIEnv *env, jobject object, jfloat freq, jfloat bw, jint nodeId){

    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(nodeId, 0, freq);
    request.set(nodeId, 1, bw);    
    request.closeBundle();
    request.send(); 
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_delayPlay(JNIEnv *env, jobject object, jintArray nodeId, jfloat maxDelayTime, jfloat delayTime, jfloat feedBack){
    
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0);
    jintArray returnId = env->NewIntArray(2);
    jint *m_returnId = env->GetIntArrayElements(returnId, NULL); 

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    
    auto synth = request.synth(
            METHCLA_PLUGINS_DELAY_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {delayTime, feedBack},
            {Methcla::Value(maxDelayTime)}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());

    request.closeBundle();
    request.send();

    m_returnId[0]=synth.id();
    m_returnId[1]=m_nodeId[1];

    env->ReleaseIntArrayElements(returnId, m_returnId, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return returnId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_delaySet(JNIEnv *env, jobject object, jfloat delayTime, jfloat feedBack, jint nodeId){
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    request.set(nodeId, 0, delayTime);
    request.set(nodeId, 1, feedBack);
    request.closeBundle();
    request.send(); 
};

JNIEXPORT jintArray JNICALL Java_processing_sound_MethClaInterface_reverbPlay(JNIEnv *env, jobject object, jintArray nodeId, jfloat room, jfloat damp, jfloat wet){

    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    jintArray returnId = env->NewIntArray(2);
    jint *m_returnId = env->GetIntArrayElements(returnId, NULL);

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();
    Methcla::Request request(engine());
    
    float dry = 1-wet;

    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_REVERB_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {room, damp, wet, dry},
            {}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());

    request.closeBundle();
    request.send();
    
    m_returnId[0]=synth.id();
    m_returnId[1]=m_nodeId[1];

    env->ReleaseIntArrayElements(returnId, m_returnId, 0);
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return returnId;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_reverbSet(JNIEnv *env, jobject object, jfloat room, jfloat damp, jfloat wet, jint nodeId){
    Methcla::Request request(engine());
    float dry = 1-wet;

    request.openBundle(Methcla::immediately);
    request.set(nodeId, 0, room);
    request.set(nodeId, 1, damp);
    request.set(nodeId, 2, wet);
    request.set(nodeId, 3, dry);
    request.closeBundle();
    request.send(); 
};


JNIEXPORT jlong JNICALL Java_processing_sound_MethClaInterface_amplitude(JNIEnv *env, jobject object, jintArray nodeId){

    jlong ptr;

    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();

    Methcla::Request request(engine());

    ServerValue * amp_ptr = new ServerValue; 

    ptr = (jlong)amp_ptr;

    request.openBundle(Methcla::immediately);

    auto synth = request.synth(
            METHCLA_PLUGINS_AMPLITUDE_FOLLOWER_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {},
            {}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);

    request.activate(synth.id());
    request.closeBundle();
    request.send();

    auto id = engine().addNotificationHandler([amp_ptr](const OSCPP::Server::Message& msg) {
        if (msg == "/amplitude") {
           OSCPP::Server::ArgStream args(msg.args());
           std::lock_guard<std::mutex> guard(mutex_amp_in);
           while (!args.atEnd()) {    
                amp_ptr->amp = args.float32();
           }
           return false;
        }
        return false;
    });

    amp_ptr->id = id;
    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return ptr;
};

JNIEXPORT jfloat JNICALL Java_processing_sound_MethClaInterface_poll_1amplitude(JNIEnv * env, jobject object, jlong ptr){
    ServerValue *amp_ptr = (ServerValue*)ptr; 
    std::lock_guard<std::mutex> guard(mutex_amp_out);
    return amp_ptr->amp;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_destroy_1amplitude(JNIEnv *env, jobject object, jlong ptr){

    ServerValue *amp_ptr = (ServerValue*)ptr;
    engine().removeNotificationHandler(amp_ptr->id);
    delete amp_ptr;
};

JNIEXPORT jlong JNICALL Java_processing_sound_MethClaInterface_fft(JNIEnv *env, jobject object, jintArray nodeId, jint fftSize){

    jlong ptr;
    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    //ServerArray *fft_ptr = (ServerArray *) malloc(sizeof(ServerArray));

    ServerArray * fft_ptr = new ServerArray; 
    
    fft_ptr->fft.resize(fftSize, 0);

    fft_ptr->fftSize=fftSize;
    ptr = (jlong)fft_ptr;

    Methcla::Engine::NotificationHandler msg;

    Methcla::AudioBusId in_bus = m_engine->audioBusId().alloc();
    Methcla::AudioBusId out_bus = m_engine->audioBusId().alloc();
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);

    std::cout << fftSize << std::endl;

    auto synth = request.synth(
            METHCLA_PLUGINS_FFT_URI,
            Methcla::NodePlacement::after(m_nodeId[0]),
            {},
            {Methcla::Value(fftSize)}
    );

    request.mapOutput(m_nodeId[0], 0, in_bus);
    request.mapInput(synth.id(), 0, in_bus);
    request.mapOutput(synth.id(), 0, out_bus);
    request.mapInput(m_nodeId[1], 0, out_bus);  

    request.activate(synth.id());
    request.closeBundle();
    request.send();

    auto id = engine().addNotificationHandler([fft_ptr](const OSCPP::Server::Message& msg) {
        if (msg == "/fft") {
           OSCPP::Server::ArgStream args(msg.args());
           int i=0;
           {
               std::lock_guard<std::mutex> guard(mutex_fft_in);
               while (!args.atEnd()) { 
                  fft_ptr->fft[i] = args.float32();
                  i++;
               }
            }
            return false;
        }
        return false;
    });

    fft_ptr->id = id;

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);

    return ptr;
};

JNIEXPORT jfloatArray JNICALL Java_processing_sound_MethClaInterface_poll_1fft(JNIEnv *env, jobject object, jlong ptr){
    
    ServerArray *fft_ptr = (ServerArray*)ptr; 
    jfloatArray fft_mag = env->NewFloatArray(fft_ptr->fftSize);
    jfloat *m_fft_mag = env->GetFloatArrayElements(fft_mag, NULL);

    std::lock_guard<std::mutex> guard(mutex_fft_out);
    for (int i = 0; i < fft_ptr->fftSize; ++i)
    {
        m_fft_mag[i]=fft_ptr->fft[i];
    }

    env->ReleaseFloatArrayElements(fft_mag, m_fft_mag, 0);

    return fft_mag;
};

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_destroy_1fft(JNIEnv *env, jobject object, jlong ptr){
    ServerArray * fft_ptr = (ServerArray*)ptr;
    engine().removeNotificationHandler(fft_ptr->id);
    delete fft_ptr;
};

/* OLD VARIABLE IN OUT FUNCTION
JNIEXPORT jint JNICALL Java_processing_sound_MethClaInterface_out(JNIEnv *env, jobject object, jint in, jint out, jfloatArray pos){
    
    float* n_pos = (float *)env->GetFloatArrayElements(pos, 0); 
    std::vector<float> control (in, 0.f);

    for (int i = 0; i < in; ++i){control[i]=n_pos[i];}
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);
    auto synth = request.synth(
            METHCLA_PLUGINS_PAN2_URI, 
            engine().root(), 
            control,
            {Methcla::Value(in), Methcla::Value(out)}
    );
    //request.mapOutput(synth.id(), 0, Methcla::AudioBusId(0), Methcla::kBusMappingExternal);
    request.activate(synth.id());
    request.closeBundle();
    request.send();
        
    env->ReleaseFloatArrayElements(pos, n_pos, 0); 

    return synth.id();
};
*/

JNIEXPORT void JNICALL Java_processing_sound_MethClaInterface_out(JNIEnv *env, jobject object, jint out, jintArray nodeId){

    jint* m_nodeId = env->GetIntArrayElements(nodeId, 0); 
    
    Methcla::Request request(engine());
    request.openBundle(Methcla::immediately);

    request.mapOutput(m_nodeId[0], 0, Methcla::AudioBusId(out), Methcla::kBusMappingExternal);

    request.closeBundle();
    request.send();

    env->ReleaseIntArrayElements(nodeId, m_nodeId, 0);  
};

