/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: '/api/v1/bff/:path*',
        destination: `${process.env.BFF_URL || 'http://bff:8080'}/api/v1/bff/:path*`,
      },
    ]
  },
}

module.exports = nextConfig
