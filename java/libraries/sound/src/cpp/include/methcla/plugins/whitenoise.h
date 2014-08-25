//
//  whitenoise.h
//  
//
//  Created by wirsing on 13.12.13.
//
//


#ifndef METHCLA_PLUGINS_WHITE_NOISE_H_INCLUDED
#define METHCLA_PLUGINS_WHITE_NOISE_H_INCLUDED

#include <methcla/plugin.h>

METHCLA_EXPORT const Methcla_Library* methcla_plugins_white_noise(const Methcla_Host*, const char*);

#define METHCLA_PLUGINS_WHITE_NOISE_URI METHCLA_PLUGINS_URI "/white_noise"

#endif // METHCLA_PLUGINS_WHITE_NOISE_H_INCLUDED