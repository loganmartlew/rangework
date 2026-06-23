<svelte:options runes={true} />

<script lang="ts">
  import { onMount, type Snippet } from 'svelte';
  import {
    resolveConsentState,
    submitDecision,
    getErrorMessage,
    type ConsentState,
  } from './consent-logic.js';

  let { logo }: { logo: Snippet } = $props();

  let state: ConsentState = $state({ kind: 'loading' });
  let submitting = $state(false);

  async function initialize() {
    state = { kind: 'loading' };
    const result = await resolveConsentState();

    if (result.kind === 'redirect') {
      window.location.replace(result.url);
      return;
    }

    state = result;
  }

  async function handleDecision(decision: 'approve' | 'deny') {
    if (state.kind !== 'consent') return;
    submitting = true;

    const result = await submitDecision(state.authorizationId, decision);

    if ('redirect_url' in result) {
      window.location.replace(result.redirect_url);
      return;
    }

    submitting = false;
    state = { kind: 'error', error: result.error };
  }

  onMount(() => {
    initialize();
  });
</script>

<div class="w-full max-w-sm">
  <!-- Wordmark -->
  <div class="mb-8 flex items-center justify-center gap-2.5" aria-hidden="true">
    {@render logo()}
    <span class="text-[1.05rem] font-medium tracking-[-0.01em] text-onbackground">
      Rangework
    </span>
  </div>

  <!-- Main card -->
  <div class="rounded-2xl border border-(--card-border) bg-surface px-6 py-6 shadow-sm">
    {#if state.kind === 'loading'}
      <!-- Loading state -->
      <p
        class="animate-pulse text-center text-[0.93rem] text-(--body-soft)"
        aria-live="polite"
        aria-busy="true"
      >
        Checking authorization&hellip;
      </p>
    {:else if state.kind === 'consent'}
      <!-- Consent state -->
      <h1 class="mb-1 text-[1.1rem] font-medium text-onsurface">Authorize access</h1>
      <p class="mb-5 text-[0.88rem] text-(--body-soft)">
        <strong class="font-medium text-onsurface">{state.clientName}</strong>
        {' '}wants to access your Rangework account.
      </p>

      <div class="mb-5 rounded-xl border border-(--card-border) bg-background px-4 py-3">
        <p
          class="mb-2 font-mono text-[0.7rem] uppercase tracking-widest text-(--label-muted)"
        >
          Requested permissions
        </p>
        <ul aria-label="Requested permissions" class="space-y-1.5">
          <li class="flex items-start gap-2 text-[0.88rem] text-(--body-soft)">
            <svg
              class="mt-[0.22rem] h-3.5 w-3.5 shrink-0 text-primary"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fill-rule="evenodd"
                d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                clip-rule="evenodd"
              />
            </svg>
            Manage your practice data
          </li>
        </ul>
      </div>

      <p class="mb-5 text-[0.78rem] leading-5 text-(--label-muted)">
        This will allow <span class="font-medium">{state.clientName}</span> to read and create
        practice units and sessions in your account. You can revoke access at any time.
      </p>

      <div class="flex flex-col gap-2.5">
        <button
          type="button"
          disabled={submitting}
          onclick={() => handleDecision('approve')}
          class="w-full cursor-pointer rounded-full bg-primary py-[0.8rem] text-[0.92rem] font-medium tracking-[-0.01em] text-onprimary transition-[filter,transform] duration-150 hover:brightness-105 active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
        >
          Approve
        </button>
        <button
          type="button"
          disabled={submitting}
          onclick={() => handleDecision('deny')}
          class="w-full cursor-pointer rounded-full border border-outlinevariant py-[0.8rem] text-[0.92rem] font-medium tracking-[-0.01em] text-onsurface transition-[background-color,border-color,transform] duration-150 hover:bg-(--tag-bg) active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
        >
          Deny
        </button>
      </div>
    {:else if state.kind === 'error'}
      <!-- Error state -->
      <div class="mb-4 flex h-9 w-9 items-center justify-center rounded-full bg-errorcontainer">
        <svg
          class="h-5 w-5 text-onerrorcontainer"
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path
            fill-rule="evenodd"
            d="M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Zm-8-5a.75.75 0 0 1 .75.75v4.5a.75.75 0 0 1-1.5 0v-4.5A.75.75 0 0 1 10 5Zm0 10a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"
            clip-rule="evenodd"
          />
        </svg>
      </div>
      <p class="mb-4 text-[0.93rem] text-(--body-soft)" aria-live="assertive">
        {getErrorMessage(state.error)}
      </p>
      {#if state.error === 'network' || state.error === 'auth-failed'}
        <button
          type="button"
          onclick={initialize}
          class="w-full rounded-full border border-outlinevariant py-[0.8rem] text-[0.92rem] font-medium tracking-[-0.01em] text-onsurface transition-[background-color,border-color,transform] duration-150 hover:bg-(--tag-bg) active:translate-y-px disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
        >
          Try again
        </button>
      {/if}
    {/if}
  </div>

  <p class="mt-5 text-center text-[0.78rem] text-(--label-muted)">
    Your data is private and secured by Rangework.
  </p>
</div>
