# Stage 1: Build Angular application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and build for production
COPY . .

# Pass build-time environment variables
ARG NG_APP_API_URL
ARG NG_APP_RAZORPAY_KEY
ENV NG_APP_API_URL=$NG_APP_API_URL
ENV NG_APP_RAZORPAY_KEY=$NG_APP_RAZORPAY_KEY

RUN npm run build -- --configuration=production

# Stage 2: Serve application with Nginx
FROM nginx:stable-alpine

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy build artifacts from build stage
# Angular 17+ default output path is dist/<project-name>/browser
COPY --from=build /app/dist/founderlink-frontend/browser /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
