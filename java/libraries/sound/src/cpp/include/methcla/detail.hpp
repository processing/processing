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

#ifndef METHCLA_DETAIL_HPP_INCLUDED
#define METHCLA_DETAIL_HPP_INCLUDED

#include <memory>
#include <stdexcept>
#include <string>

#include <methcla/engine.h>

namespace Methcla
{
    namespace detail
    {
        template <class D, typename T> class Id
        {
        public:
            explicit Id(T id)
                : m_id(id)
            { }
            Id(const D& other)
                : m_id(other.m_id)
            { }

            T id() const
            {
                return m_id;
            }

            bool operator==(const D& other) const
            {
                return m_id == other.m_id;
            }

            bool operator!=(const D& other) const
            {
                return m_id != other.m_id;
            }

        private:
            T m_id;
        };

        inline static void throwError(Methcla_Error err)
        {
            if (methcla_is_error(err))
            {
                if (methcla_error_has_code(err, kMethcla_ArgumentError)) {
                    std::string msg(methcla_error_message(err));
                    methcla_error_free(err);
                    throw std::invalid_argument(msg);
                } else if (methcla_error_has_code(err, kMethcla_LogicError)) {
                    std::string msg(methcla_error_message(err));
                    methcla_error_free(err);
                    throw std::logic_error(msg);
                } else if (methcla_error_has_code(err, kMethcla_MemoryError)) {
                    methcla_error_free(err);
                    throw std::bad_alloc();
                } else {
                    std::string msg(  methcla_error_message(err)
                                    ? methcla_error_message(err)
                                    : methcla_error_code_description(methcla_error_code(err)));
                    methcla_error_free(err);
                    throw std::runtime_error(msg);
                }
            }
        }

        inline static void checkReturnCode(Methcla_Error err)
        {
            throwError(err);
        }

        template <typename T> T combineFlags(T a, T b)
        {
            // FIXME: Not available in GCC 4.6, Clang 3.3
            // typedef typename std::underlying_type<T>::type enum_type;
            typedef int enum_type;
            static_assert(sizeof(T) <= sizeof(enum_type), "combineFlags: Cannot determine underlying enum type");
            return static_cast<T>(static_cast<enum_type>(a) | static_cast<enum_type>(b));
        }
    }
}

#endif // METHCLA_DETAIL_HPP_INCLUDED
