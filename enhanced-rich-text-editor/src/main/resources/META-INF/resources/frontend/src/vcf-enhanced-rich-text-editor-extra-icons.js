import '@vaadin/icon/vaadin-iconset.js';

const template = document.createElement('template');

template.innerHTML = `<vaadin-iconset name="vcf-erte-extra-icons" size="24">
  <svg><defs>
    <g id="align-center-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-align-center" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <line x1="4" y1="6" x2="20" y2="6" />
  <line x1="8" y1="12" x2="16" y2="12" />
  <line x1="6" y1="18" x2="18" y2="18" />
</svg></g>
    <g id="align-justify-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-align-justified" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <line x1="4" y1="6" x2="20" y2="6" />
  <line x1="4" y1="12" x2="20" y2="12" />
  <line x1="4" y1="18" x2="20" y2="18" />
</svg></g>
    <g id="align-left-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-align-left" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <line x1="4" y1="6" x2="20" y2="6" />
  <line x1="4" y1="12" x2="14" y2="12" />
  <line x1="4" y1="18" x2="18" y2="18" />
</svg></g>
    <g id="align-right-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-align-right" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <line x1="4" y1="6" x2="20" y2="6" />
  <line x1="10" y1="12" x2="20" y2="12" />
  <line x1="6" y1="18" x2="20" y2="18" />
</svg></g>
    <g id="deindent-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-indent-decrease" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <line x1="20" y1="6" x2="13" y2="6" />
  <line x1="20" y1="12" x2="11" y2="12" />
  <line x1="20" y1="18" x2="13" y2="18" />
  <path d="M8 8l-4 4l4 4" />
</svg></g>
    <g id="indent-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-indent-increase" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <line x1="20" y1="6" x2="9" y2="6" />
  <line x1="20" y1="12" x2="13" y2="12" />
  <line x1="20" y1="18" x2="9" y2="18" />
  <path d="M4 8l4 4l-4 4" />
</svg></g>
    <g id="lock-icon"><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-lock" width="24" height="24" viewBox="0 0 24 24" stroke-width="1.5" stroke="var(--rte-extra-icons-stroke-color)" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
  <rect x="5" y="11" width="14" height="10" rx="2" />
  <circle cx="12" cy="16" r="1" />
  <path d="M8 11v-4a4 4 0 0 1 8 0v4" />
</svg></g>
    <g id="table-icon">
        <svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="var(--rte-extra-icons-stroke-color)">
            <path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h133v-133H200v133Zm213 0h134v-133H413v133Zm214 0h133v-133H627v133ZM200-413h133v-134H200v134Zm213 0h134v-134H413v134Zm214 0h133v-134H627v134ZM200-627h133v-133H200v133Zm213 0h134v-133H413v133Zm214 0h133v-133H627v133Z"/>
        </svg>
    </g>
    <g id="table-rows-icon">
        <svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="var(--rte-extra-icons-stroke-color)">
            <path d="M760-200v-120H200v120h560Zm0-200v-160H200v160h560Zm0-240v-120H200v120h560ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Z"/>
        </svg>
    </g>
    <g id="table-cols-icon">
        <svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="var(--rte-extra-icons-stroke-color)">
            <path d="M160-240h160v-480H160v480Zm240 0h160v-480H400v480Zm240 0h160v-480H640v480Zm-480 80q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h640q33 0 56.5 23.5T880-720v480q0 33-23.5 56.5T800-160H160Z"/>
        </svg>
    </g>
  </defs></svg>
</vaadin-iconset>`;

document.head.appendChild(template.content);
