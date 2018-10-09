# Thumbly [![Build Status](https://travis-ci.org/code13k/thumbly.svg?branch=master)](https://travis-ci.org/code13k/thumbly)
**Thumbly** is a on-demand image server for generating thumbnail dynamically.
It generate thumbnail image on request and generated thumbnail are cached in disk.
So it respond thumbnail image quickly.

It provide secret url that is automatically deleted after specific time.
You can provide secret url to client for security.

It provide clustering nodes using Hazelcast.
You can build high availability(HA) systems by clustering node.


* **[Configuration](./doc/configuration.md)**
* **[Get Thumbnail](./doc/main_server.md)**
* **[API](./doc/api_server.md)**


# Latest Release
The current stable version is ready.

The current unstable version is [v1.0.0-Alpha.2](https://github.com/code13k/thumbly/releases/tag/1.0.0-Alpha.2)


# Supported Image Format
* JPG
* GIF
* PNG
* WEBP


# Dependency
* Imagemagick
  * https://www.imagemagick.org
  * Thumbly use Imagemagick for image processing
 

# License
MIT License

Copyright (c) 2018 Code13K

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

