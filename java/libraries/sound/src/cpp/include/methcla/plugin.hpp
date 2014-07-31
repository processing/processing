// Copyright 2013 Samplecount S.L.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#ifndef METHCLA_PLUGIN_HPP_INCLUDED
#define METHCLA_PLUGIN_HPP_INCLUDED

#include <methcla/log.hpp>
#include <methcla/plugin.h>
#include <oscpp/server.hpp>

#include <functional>
#include <cstring>

// NOTE: This API is unstable and subject to change!

namespace Methcla { namespace Plugin {

    template <class Synth> class World
    {
        const Methcla_World* m_context;

    public:
        World(const Methcla_World* context)
            : m_context(context)
        { }

        double sampleRate() const
        {
            return methcla_world_samplerate(m_context);
        }

        size_t blockSize() const
        {
            return methcla_world_block_size(m_context);
        }

        Methcla_Time currentTime() const
        {
            return methcla_world_current_time(m_context);
        }

        void* alloc(size_t size) const
        {
            return methcla_world_alloc(m_context, size);
        }

        void* allocAligned(size_t alignment, size_t size) const
        {
            return methcla_world_alloc_aligned(m_context, alignment, size);
        }

        void free(void* ptr)
        {
            methcla_world_free(m_context, ptr);
        }

        void performCommand(Methcla_HostPerformFunction perform, void* data)
        {
            methcla_world_perform_command(m_context, perform, data);
        }

        LogStream log(Methcla_LogLevel logLevel=kMethcla_LogInfo)
        {
            using namespace std::placeholders;
            return LogStream(std::bind(m_context->log_line, m_context, _1, _2), logLevel);
        }

        void synthRetain(Synth* synth) const
        {
            methcla_world_synth_retain(m_context, synth);
        }

        void synthRelease(Synth* synth) const
        {
            methcla_world_synth_release(m_context, synth);
        }

        void synthDone(Synth* synth) const
        {
            methcla_world_synth_done(m_context, synth);
        }
    };

    class HostContext
    {
        const Methcla_Host* m_context;

    public:
        HostContext(const Methcla_Host* context)
            : m_context(context)
        {}

        LogStream log(Methcla_LogLevel logLevel=kMethcla_LogInfo)
        {
            using namespace std::placeholders;
            return LogStream(std::bind(m_context->log_line, m_context, _1, _2), logLevel);
        }
    };

    class NoPorts
    {
    public:
        enum Port { };

        static size_t numPorts() { return 0; }

        static Methcla_PortDescriptor descriptor(Port)
        {
            Methcla_PortDescriptor result;
            std::memset(&result, 0, sizeof(result));
            return result;
        }
    };

    class PortDescriptor
    {
    public:
        static Methcla_PortDescriptor make(Methcla_PortDirection direction, Methcla_PortType type, Methcla_PortFlags flags=kMethcla_PortFlags)
        {
            Methcla_PortDescriptor pd;
            pd.direction = direction;
            pd.type = type;
            pd.flags = flags;
            return pd;
        }

        static Methcla_PortDescriptor audioInput(Methcla_PortFlags flags=kMethcla_PortFlags)
        {
            return make(kMethcla_Input, kMethcla_AudioPort, flags);
        }

        static Methcla_PortDescriptor audioOutput(Methcla_PortFlags flags=kMethcla_PortFlags)
        {
            return make(kMethcla_Output, kMethcla_AudioPort, flags);
        }

        static Methcla_PortDescriptor controlInput(Methcla_PortFlags flags=kMethcla_PortFlags)
        {
            return make(kMethcla_Input, kMethcla_ControlPort, flags);
        }

        static Methcla_PortDescriptor controlOutput(Methcla_PortFlags flags=kMethcla_PortFlags)
        {
            return make(kMethcla_Output, kMethcla_ControlPort, flags);
        }
    };

    template <class Options, class PortDescriptor> class StaticSynthOptions
    {
    public:
        typedef Options Type;

        static void
        configure( const void* tag_buffer
                 , size_t tag_buffer_size
                 , const void* arg_buffer
                 , size_t arg_buffer_size
                 , Methcla_SynthOptions* options )
        {
            OSCPP::Server::ArgStream args(
                OSCPP::ReadStream(tag_buffer, tag_buffer_size),
                OSCPP::ReadStream(arg_buffer, arg_buffer_size)
            );
            new (options) Type(args);
        }

        static bool
        port_descriptor( const Methcla_SynthOptions*
                       , Methcla_PortCount index
                       , Methcla_PortDescriptor* port )
        {
            if (index < PortDescriptor::numPorts())
            {
                *port = PortDescriptor::descriptor(static_cast<typename PortDescriptor::Port>(index));
                return true;
            }
            return false;
        }
    };

    namespace detail
    {
        template <class Synth, bool Condition>
        class IfSynthDefHasActivate
        {
        public:
            static inline void exec(const Methcla_World*, Synth*) { }
        };

        template <class Synth>
        class IfSynthDefHasActivate<Synth, true>
        {
        public:
            static inline void exec(const Methcla_World* context, Synth* synth)
                { synth->activate(World<Synth>(context)); }
        };

        template <class Synth, bool Condition>
        class IfSynthDefHasCleanup
        {
        public:
            static inline void exec(const Methcla_World*, Synth*) { }
        };

        template <class Synth>
        class IfSynthDefHasCleanup<Synth, true>
        {
        public:
            static inline void exec(const Methcla_World* context, Synth* synth)
                { synth->cleanup(World<Synth>(context)); }
        };
    } // namespace detail

    enum SynthDefFlags
    {
        kSynthDefDefaultFlags = 0x00,
        kSynthDefHasActivate  = 0x01,
        kSynthDefHasCleanup   = 0x02
    };

    template <class Synth, class Options, class PortDescriptor, SynthDefFlags Flags=kSynthDefDefaultFlags> class SynthDef
    {
        static void
        construct( const Methcla_World* context
                 , const Methcla_SynthDef* synthDef
                 , const Methcla_SynthOptions* options
                 , Methcla_Synth* synth )
        {
            assert(context != nullptr);
            assert(options != nullptr);
            new (synth) Synth(World<Synth>(context), synthDef, *static_cast<const typename Options::Type*>(options));
        }

        static void
        connect( Methcla_Synth* synth
               , Methcla_PortCount port
               , void* data)
        {
            static_cast<Synth*>(synth)->connect(static_cast<typename PortDescriptor::Port>(port), data);
        }

        static void
        activate(const Methcla_World* context, Methcla_Synth* synth)
        {
            detail::IfSynthDefHasActivate<
                Synth,
                (Flags & kSynthDefHasActivate) == kSynthDefHasActivate
            >::exec(context, static_cast<Synth*>(synth));
        }

        static void
        process(const Methcla_World* context, Methcla_Synth* synth, size_t numFrames)
        {
            static_cast<Synth*>(synth)->process(World<Synth>(context), numFrames);
        }

        static void
        destroy(const Methcla_World* context, Methcla_Synth* synth)
        {
            // Call cleanup method
            detail::IfSynthDefHasActivate<
                Synth,
                (Flags & kSynthDefHasCleanup) == kSynthDefHasCleanup
            >::exec(context, static_cast<Synth*>(synth));
            // Call destructor
            static_cast<Synth*>(synth)->~Synth();
        }

    public:
        void operator()(const Methcla_Host* host, const char* uri)
        {
            static const Methcla_SynthDef kSynthDef =
            {
                uri,
                sizeof(Synth),
                sizeof(typename Options::Type),
                Options::configure,
                Options::port_descriptor,
                construct,
                connect,
                activate,
                process,
                destroy
            };
            methcla_host_register_synthdef(host, &kSynthDef);
        }
    };

    template <class Synth, class Options, class Ports, SynthDefFlags Flags=kSynthDefDefaultFlags>
        using StaticSynthDef
        = SynthDef<Synth, StaticSynthOptions<Options,Ports>, Ports, Flags>;
} }

#endif // METHCLA_PLUGIN_HPP_INCLUDED
