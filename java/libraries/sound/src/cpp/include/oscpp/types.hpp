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

#ifndef OSCPP_TYPES_HPP_INCLUDED
#define OSCPP_TYPES_HPP_INCLUDED

namespace OSCPP {

class Blob
{
public:
    Blob()
        : m_size(0), m_data(nullptr)
    { }
    Blob(const void* data, size_t size)
        : m_size(size), m_data(data)
    { }
    Blob(const Blob& other) = default;

    size_t size() const { return m_size; }
    const void* data() const { return m_data; }

private:
    size_t      m_size;
    const void* m_data;
};

}

#endif // OSCPP_TYPES_HPP_INCLUDED
