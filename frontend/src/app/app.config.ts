import { importProvidersFrom, ApplicationConfig } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { NgxEchartsModule } from 'ngx-echarts';
import * as echarts from 'echarts';

import { routes } from './app.routes';
import { authTokenInterceptor } from './core/auth/auth-token.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([authTokenInterceptor])),
    importProvidersFrom(NgxEchartsModule.forRoot({ echarts }))
  ]
};
