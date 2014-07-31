// oscpp library
//
// Copyright (c) 2004-2013 Stefan Kersten <sk@k-hornz.de>
//
// Permission is hereby granted, free of charge, to any person or organization
// obtaining a copy of the software and accompanying documentation covered by
// this license (the "Software") to use, reproduce, display, distribute,
// execute, and transmit the Software, and to prepare derivative works of the
// Software, and to permit third-parties to whom the Software is furnished to
// do so, all subject to the following:
//
// The copyright notices in the Software and this entire statement, including
// the above license grant, this restriction and the following disclaimer,
// must be included in all copies of the Software, in whole or in part, and
// all derivative works of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
// SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
// FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

#ifndef OSCPP_ERROR_HPP_INCLUDED
#define OSCPP_ERROR_HPP_INCLUDED

#include <exception>
#include <string>

namespace OSCPP {

class Error : public std::exception
{
public:
    Error(const std::string& what)
        : m_what(what)
    { }

    virtual ~Error() noexcept
    { }

    const char* what() const noexcept override
    {
        return m_what.c_str();
    }

private:
    std::string m_what;
};

class UnderrunError : public Error
{
public:
    UnderrunError()
        : Error(std::string("Buffer underrun"))
    { }
};

class OverflowError : public Error
{
public:
    OverflowError(size_t bytes)
        : Error(std::string("Buffer overflow")),
          m_bytes(bytes)
    { }

    size_t numBytes() const { return m_bytes; }

private:
    size_t m_bytes;
};

class ParseError : public Error
{
public:
    ParseError(const std::string& what="Parse error")
        : Error(what)
    { }
};

}

#endif // OSCPP_ERROR_HPP_INCLUDED
