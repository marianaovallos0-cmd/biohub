export function Logo() {
  return (
    <span className="flex items-center gap-2.5">
      <svg aria-hidden="true" width="30" height="30" viewBox="0 0 32 32">
        <path d="M16 3 C8 6 4 13 5 22 C5.5 26 9 29 13 29 C22 28 28 19 27 6 C23 4 19 3 16 3 Z" className="fill-brote" />
        <path d="M8 27 C13 19 18 13 25 7 M13 20 L11 14 M17 16 L21 17" fill="none" className="stroke-bosque" strokeWidth="1.6" strokeLinecap="round" />
      </svg>
      <span className="font-heading text-[22px] font-extrabold tracking-tight text-white">BioHub</span>
    </span>
  );
}