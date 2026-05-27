/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: false,
  },
  images: {
    unoptimized: true,
  },
  async rewrites() {
    return [
      // API gateway has no route for hub-routes or ai-slack — call services directly
      {
        source: '/api/v1/hub-routes/:path*',
        destination: 'http://localhost:8082/api/v1/hub-routes/:path*',
      },
      {
        source: '/api/v1/hub-routes',
        destination: 'http://localhost:8082/api/v1/hub-routes',
      },
      {
        source: '/api/v1/:path*',
        destination: 'http://localhost:8000/api/v1/:path*',
      },
    ];
  },
}

export default nextConfig
