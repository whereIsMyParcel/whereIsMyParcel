import NextAuth, { AuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

export const authOptions: AuthOptions = {
  providers: [
    CredentialsProvider({
      name: "Keycloak",
      credentials: {
        username: { label: "Username", type: "text", placeholder: "master01" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.username || !credentials?.password) return null;

        try {
          // Keycloak Password Grant Token URL
          const tokenUrl = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`;
          const params = new URLSearchParams({
            client_id: process.env.KEYCLOAK_ID || "local-dev-client",
            grant_type: "password",
            username: credentials.username,
            password: credentials.password,
          });

          if (process.env.KEYCLOAK_SECRET) {
            params.append("client_secret", process.env.KEYCLOAK_SECRET);
          }

          const res = await fetch(tokenUrl, {
            method: "POST",
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
            },
            body: params.toString(),
          });

          const data = await res.json();

          if (res.ok && data.access_token) {
            // Return user object including the access token
            return {
              id: credentials.username,
              name: credentials.username,
              access_token: data.access_token,
            } as any;
          }
          
          return null;
        } catch (e) {
          console.error("Keycloak Login Error:", e);
          return null;
        }
      },
    }),
  ],
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.accessToken = (user as any).access_token;
      }
      return token;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string;
      return session;
    },
  },
  pages: {
    // optional: customize signin page
  }
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };
