/*
    Copyright 2012-2013 Samplecount S.L.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

#ifndef METHCLA_PLUGINS_SAW_H_INCLUDED
#define METHCLA_PLUGINS_SAW_H_INCLUDED

#include <methcla/plugin.h>

METHCLA_EXPORT const Methcla_Library* methcla_plugins_saw(const Methcla_Host*, const char*);
#define METHCLA_PLUGINS_SAW_URI METHCLA_PLUGINS_URI "/saw"

#endif /* METHCLA_PLUGINS_SAW_H_INCLUDED */
