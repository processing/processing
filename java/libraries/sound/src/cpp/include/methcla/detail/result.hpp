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

#ifndef METHCLA_DETAIL_RESULT_HPP_INCLUDED
#define METHCLA_DETAIL_RESULT_HPP_INCLUDED

#include <methcla/common.h>
#include <oscpp/server.hpp>

#include <condition_variable>
#include <mutex>
#include <sstream>
#include <string>
#include <thread>

namespace Methcla
{
    namespace detail
    {
        class ResultBase
        {
            std::condition_variable m_cond_var;

        protected:
            std::mutex              m_mutex;
            bool                    m_cond;
            Methcla_ErrorCode       m_error;
            std::string             m_errorMessage;

        public:
            ResultBase()
                : m_cond(false)
                , m_error(kMethcla_NoError)
            { }

            ResultBase(const ResultBase&) = delete;
            ResultBase& operator=(const ResultBase&) = delete;

            void checkResponse(const char* requestAddress, const OSCPP::Server::Message& msg)
            {
                if (msg == "/error")
                {
                    auto args(msg.args());
                    Methcla_ErrorCode errorCode = static_cast<Methcla_ErrorCode>(args.int32());
                    const char* errorMessage = args.string();
                    setError(errorCode, errorMessage);
                }
                else if (msg != requestAddress)
                {
                    std::stringstream s;
                    s << "Unexpected response message address " << msg.address() << " (expected " << requestAddress << ")";
                    setError(kMethcla_LogicError, s.str().c_str());
                }
            }

        protected:
            inline void notify()
            {
                m_cond = true;
                m_cond_var.notify_one();
            }

            inline void wait()
            {
                std::unique_lock<std::mutex> lock(m_mutex);
                while (!m_cond) {
                    m_cond_var.wait(lock);
                }
                if (m_error != kMethcla_NoError) {
                    throwError(methcla_error_new_with_message(m_error, m_errorMessage.c_str()));
                }
            }

            void setError(Methcla_ErrorCode error, const char* message)
            {
                std::lock_guard<std::mutex> lock(m_mutex);
                if (m_cond)
                {
                    m_error = kMethcla_LogicError;
                    m_errorMessage = "Result error already set";
                }
                else
                {
                    m_error = error;
                    m_errorMessage = message;
                }
                notify();
            }
        };

        template <class T> class Result : public ResultBase
        {
        public:
            void set(Methcla_ErrorCode error, const char* message)
            {
                setError(error, message);
            }

            void set(const T& value)
            {
                std::lock_guard<std::mutex> lock(m_mutex);
                if (m_error == kMethcla_NoError)
                {
                    if (m_cond)
                    {
                        m_error = kMethcla_LogicError;
                        m_errorMessage = "Result already set";
                    }
                    else
                    {
                        m_value = value;
                        notify();
                    }
                }
            }

            const T& get()
            {
                wait();
                return m_value;
            }

        private:
            T m_value;
        };

        template <> class Result<void> : public ResultBase
        {
        public:
            void set(Methcla_ErrorCode error, const char* message)
            {
                setError(error, message);
            }

            void set()
            {
                std::lock_guard<std::mutex> lock(m_mutex);
                if (m_error == kMethcla_NoError)
                {
                    if (m_cond)
                    {
                        m_error = kMethcla_LogicError;
                        m_errorMessage = "Result already set";
                    }
                    else
                    {
                        notify();
                    }
                }
            }

            void get()
            {
                wait();
            }
        };
    }
}

#endif // METHCLA_DETAIL_RESULT_HPP_INCLUDED
